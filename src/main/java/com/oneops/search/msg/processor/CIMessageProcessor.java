/*******************************************************************************
 *  
 *   Copyright 2015 Walmart, Inc.
 *  
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *  
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  
 *******************************************************************************/
package com.oneops.search.msg.processor;

import com.google.gson.Gson;
import com.oneops.antenna.domain.NotificationMessage;
import com.oneops.cms.cm.domain.CmsCI;
import com.oneops.cms.dj.domain.CmsRelease;
import com.oneops.cms.simple.domain.CmsCISimple;
import com.oneops.cms.simple.domain.CmsWorkOrderSimple;
import com.oneops.cms.util.CmsConstants;
import com.oneops.search.domain.CmsCISearch;
import com.oneops.search.domain.CmsDeploymentPlan;
import com.oneops.search.domain.CmsNotificationSearch;
import com.oneops.search.domain.CmsReleaseSearch;
import com.oneops.search.msg.index.Indexer;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.dozer.DozerBeanMapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.queryString;

public class CIMessageProcessor {
	public static final String CMS_ALL = "cms-all";
	private SimpleDateFormat dt = new SimpleDateFormat( "yyyy-'w'ww" );
	private static Logger logger = Logger.getLogger(CIMessageProcessor.class);
	private ElasticsearchTemplate template;
	private DozerBeanMapper mapper;
	private Gson gson = new Gson();
	private Client client;
	private final int RETRY_COUNT = 5 ;
	private static final long TIME_TO_WAIT = 5000 ;
	private static final int LOOKBACK_WEEKS = 3;
	
	
	/**
	 * 
	 * @param ci
	 * @param searchGson
	 * @return
	 */
	public String processCIMsg(CmsCISimple ci, Gson searchGson) {
		CmsCISearch ciSearch = mapper.map(ci, CmsCISearch.class);
		CmsWorkOrderSimple wo = fetchWoForCi(ciSearch.getCiId());
		if(wo != null){
			ciSearch.setWorkorder(wo);
		}else{
			logger.info("WO not found for ci " + ci.getCiId() + " of type " + ci.getCiClassName());
		}
		NotificationMessage opsNotification = ciSearch.getOps();
		if(opsNotification != null){
			ciSearch.setOps(opsNotification);
		}
		return searchGson.toJson(ciSearch);
		}
	
	
	/**
	 * Update ops notifications to CIs
	 * @param notificationMsg
	 * @param indexer
	 * @param searchGson
	 */
	public void processNotificationMsg(CmsNotificationSearch notificationMsg,Indexer indexer,Gson searchGson){
			String id = String.valueOf(notificationMsg.getCmsId());
			CmsCISearch ciSearch = fetchCIRecord(id);
			
			if(ciSearch != null){
				if("bom.Compute".equals(ciSearch.getCiClassName())){
					String hypervisor = ciSearch.getCiAttributes().get("hypervisor");
					if(hypervisor != null){
						notificationMsg.setHypervisor(hypervisor);
					}
				}
				
				ciSearch.setOps(notificationMsg);
				indexer.index(id, "ci", searchGson.toJson(ciSearch));
				logger.info("updated ops notification for ci id::" + id);
			}
			else{
				logger.warn("ci record not found for id::" + id);
			}
	}
	
	


	/**
	 * 
	 * @param indexer
	 * @param searchGson
	 * @return
	 */
	public void processDeploymentPlanMsg(CmsCI ci, Indexer indexer, Gson searchGson){
		CmsDeploymentPlan deploymentPlan = null;
		
		try {
			int genTime = extractTimeTaken(ci.getComments()).intValue();
			CmsRelease release = fetchReleaseRecord(ci.getNsPath()+"/"+ci.getCiName()+"/bom",
					ci.getUpdated(),genTime);
			if(release!=null){
				deploymentPlan = new CmsDeploymentPlan();
				deploymentPlan.setPlanGenerationTime(genTime);
				deploymentPlan.setCreatedBy(ci.getCreatedBy());
				deploymentPlan.setCiId(ci.getCiId());
				deploymentPlan.setCreated(release.getCreated());
				deploymentPlan.setCreatedBy(release.getCreatedBy());
				deploymentPlan.setCiClassName(ci.getCiClassName());
				deploymentPlan.setReleaseName(release.getReleaseName());
				deploymentPlan.setReleaseId(release.getReleaseId());
				deploymentPlan.setId(String.valueOf(ci.getCiId()) + String.valueOf(release.getReleaseId()));
				deploymentPlan.setCiRfcCount(release.getCiRfcCount());
				deploymentPlan.setCommitedBy(release.getCommitedBy());
				deploymentPlan.setNsId(release.getNsId());
				deploymentPlan.setNsPath(release.getNsPath());
				indexer.index(deploymentPlan.getId(),"plan", searchGson.toJson(deploymentPlan));
			}
		} catch (Exception e) {
			logger.error("Exception in processing deployment message: " + ExceptionUtils.getMessage(e));
		}
	}
	
	private static Double extractTimeTaken(String comments) {
		String timeTaken = comments.substring("SUCCESS:Generation time taken: ".length(), comments.indexOf(" seconds.")).trim();
		double planGentime = Double.parseDouble(timeTaken);
		return planGentime == 0 ? 1 : planGentime;
	}
	
	private CmsRelease fetchReleaseRecord(String nsPath,Date ts,int genTime) throws InterruptedException {
		Thread.sleep(3000);//Wait for latest 'bom' release
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(CmsConstants.SEARCH_TS_PATTERN);
		SearchQuery latestRelease = new NativeSearchQueryBuilder()
				.withIndices(CMS_ALL)
				.withTypes("release").withFilter(
						FilterBuilders.andFilter(
								FilterBuilders.queryFilter(QueryBuilders.termQuery("nsPath.keyword", nsPath)),
								FilterBuilders.queryFilter(QueryBuilders.rangeQuery("created").
										from(simpleDateFormat.format(DateUtils.addMinutes(ts, -(genTime + 10)))).
										to(simpleDateFormat.format(ts))))).
						withSort(SortBuilders.fieldSort("created").order(SortOrder.DESC)).build();
		
		List<CmsReleaseSearch> ciList = template.queryForList(latestRelease, CmsReleaseSearch.class);
		if(!ciList.isEmpty()){
			return ciList.get(0);
		}
		else{
			throw new RuntimeException("Cant find bom release for deployment plan generation event");
		}
	}
	
	
	private CmsCISearch fetchCIRecord(String id){

		SearchQuery searchQuery = new NativeSearchQueryBuilder() 
				.withIndices(CMS_ALL)
				.withTypes("ci").withQuery(queryString(id).field("ciId"))
				.build();
		
		List<CmsCISearch> ciList = template.queryForList(searchQuery, CmsCISearch.class);
		return !ciList.isEmpty()?ciList.get(0):null;
	}
	
	
	/**
	 * Fetch work-order for the given ciId
	 * @param ciId
	 * @return
	 */
	private CmsWorkOrderSimple fetchWoForCi(long ciId) {
		CmsWorkOrderSimple wos = null;
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(new Date());
		for (int week=0;week<LOOKBACK_WEEKS;week++) {
			calendar.add(Calendar.WEEK_OF_YEAR, -week);
			for (int i = 0; i < RETRY_COUNT; i++) {
				try {
					SearchResponse response = client.prepareSearch("cms")
							.setIndices("cms" + "-" + dt.format(calendar.getTime()))
							.setTypes("workorder")
							.setQuery(queryString(String.valueOf(ciId)).field("rfcCi.ciId"))
							.addSort("searchTags.responseDequeTS", SortOrder.DESC)
							.setSize(1)
							.execute()
							.actionGet();

					String cmsWo = (response.getHits().getHits().length > 0) ? response.getHits().getHits()[0].getSourceAsString() : null;
					if (cmsWo != null) {
						wos = gson.fromJson(cmsWo, CmsWorkOrderSimple.class);
						logger.info("WO found for ci id " + ciId + " in retry count " + i);
						break;
					} else {
						Thread.sleep(TIME_TO_WAIT); //wait for TIME_TO_WAIT ms and retry
					}
				} catch (Exception e) {
					logger.error("Error in retrieving WO for ci " + ciId);
				}
			}
		}
		return wos;
	}


	public ElasticsearchTemplate getTemplate() {
		return template;
	}

	public void setTemplate(ElasticsearchTemplate template) {
		this.template = template;
	}


	public DozerBeanMapper getMapper() {
		return mapper;
	}


	public void setMapper(DozerBeanMapper mapper) {
		this.mapper = mapper;
	}


	public void setClient(Client client) {
		this.client = client;
	}


}
