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

import com.oneops.cms.util.CmsConstants;
import com.oneops.search.domain.CmsDeploymentSearch;
import com.oneops.search.util.SearchConstants;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.queryString;

public class DpmtMessageProcessor {

    private static Logger logger = Logger.getLogger(DpmtMessageProcessor.class);
	private Client client;
	private ElasticsearchTemplate template;
	
	
	/**
	 * 
	 * @param deployment
	 * @return
	 */
	public CmsDeploymentSearch processDeploymentMsg(CmsDeploymentSearch deployment) {
		
		CmsDeploymentSearch esDeployment = null;
		try {
			esDeployment = fetchDeploymentRecord(deployment.getDeploymentId());

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(CmsConstants.SEARCH_TS_PATTERN);
			if(isFinalState(deployment.getDeploymentState())){
				
				if(esDeployment!=null && SearchConstants.DPMT_STATE_CANCELED.equalsIgnoreCase(deployment.getDeploymentState())) {
					if(SearchConstants.DPMT_STATE_PAUSED.equalsIgnoreCase(esDeployment.getDeploymentState())){
						esDeployment.setPausedEndTS(simpleDateFormat.format(new Date()));

						double pausedDuration = esDeployment.getPausedDuration() +
								((( simpleDateFormat.parse(esDeployment.getPausedEndTS()).getTime()) - simpleDateFormat.parse(esDeployment.getPausedStartTS()).getTime())/1000.0);
						esDeployment.setPausedDuration(Math.round(pausedDuration * 1000.0) / 1000.0);
						
					}else if(SearchConstants.DPMT_STATE_PENDING.equalsIgnoreCase(esDeployment.getDeploymentState())){
						esDeployment.setPendingEndTS(simpleDateFormat.format(new Date()));

						double pendingDuration = esDeployment.getPendingDuration() +
								((simpleDateFormat.parse(esDeployment.getPendingEndTS()).getTime() - ( simpleDateFormat.parse(esDeployment.getPendingStartTS()).getTime()))/1000.0);
						esDeployment.setPendingDuration(Math.round(pendingDuration * 1000.0) / 1000.0);
					}
					else if(SearchConstants.DPMT_STATE_FAILED.equalsIgnoreCase(esDeployment.getDeploymentState())){
						esDeployment.setFailedEndTS(simpleDateFormat.format(new Date()));

						double failedDuration = esDeployment.getFailedDuration() +
								((simpleDateFormat.parse(esDeployment.getFailedEndTS()).getTime() - ( simpleDateFormat.parse(esDeployment.getFailedStartTS()).getTime()))/1000.0);
						esDeployment.setFailedDuration(Math.round(failedDuration * 1000.0) / 1000.0);
					}
					}else if(esDeployment!=null && SearchConstants.DPMT_STATE_COMPLETE.equalsIgnoreCase(deployment.getDeploymentState())){
						esDeployment.setActiveEndTS(simpleDateFormat.format(new Date()));

					double activeDuration = esDeployment.getActiveDuration() +
								((( simpleDateFormat.parse(esDeployment.getActiveEndTS()).getTime()) - simpleDateFormat.parse(esDeployment.getActiveStartTS()).getTime()) / 1000.0);
						esDeployment.setActiveDuration(Math.round(activeDuration * 1000.0) / 1000.0);
					}
					esDeployment.setDeploymentState(deployment.getDeploymentState());
					esDeployment.setTotalTime((System.currentTimeMillis() - esDeployment.getCreated().getTime())/1000.0);
			}
			else if(SearchConstants.DPMT_STATE_ACTIVE.equalsIgnoreCase(deployment.getDeploymentState())){

				if(esDeployment!=null){
					esDeployment.setActiveStartTS(simpleDateFormat.format(new Date()));
					if(SearchConstants.DPMT_STATE_FAILED.equalsIgnoreCase(esDeployment.getDeploymentState())){
						esDeployment.setRetryCount(esDeployment.getRetryCount()+1);
						esDeployment.setFailedEndTS(simpleDateFormat.format(new Date()));

						double failedDuration = esDeployment.getFailedDuration() +
								((( simpleDateFormat.parse(esDeployment.getFailedEndTS()).getTime()) - simpleDateFormat.parse(esDeployment.getFailedStartTS()).getTime())/1000.0);
						esDeployment.setFailedDuration(Math.round(failedDuration * 1000.0) / 1000.0);
						esDeployment.setDeploymentState(deployment.getDeploymentState());
					}
					else if(SearchConstants.DPMT_STATE_PAUSED.equalsIgnoreCase(esDeployment.getDeploymentState())){
						esDeployment.setPausedEndTS(simpleDateFormat.format(new Date()));

						double pausedDuration = esDeployment.getPausedDuration() + 
								((( simpleDateFormat.parse(esDeployment.getPausedEndTS()).getTime()) - ( simpleDateFormat.parse(esDeployment.getPausedStartTS()).getTime()))/1000.0);
						esDeployment.setPausedDuration(Math.round(pausedDuration * 1000.0) / 1000.0);
						esDeployment.setDeploymentState(deployment.getDeploymentState());
					}
					else if(SearchConstants.DPMT_STATE_PENDING.equalsIgnoreCase(esDeployment.getDeploymentState())){
						esDeployment.setPendingEndTS(simpleDateFormat.format(new Date()));

						double pendingDuration = esDeployment.getPendingDuration() + 
								((( simpleDateFormat.parse(esDeployment.getPendingEndTS()).getTime()) - ( simpleDateFormat.parse(esDeployment.getPendingStartTS()).getTime()))/1000.0);
						esDeployment.setPendingDuration(Math.round(pendingDuration * 1000.0) / 1000.0);
						esDeployment.setDeploymentState(deployment.getDeploymentState());
					}
					updateTotalTime(esDeployment);
				}
				else{
					deployment.setActiveStartTS(simpleDateFormat.format(new Date()));
				}
			}
			else if(SearchConstants.DPMT_STATE_PENDING.equalsIgnoreCase(deployment.getDeploymentState())){
				if(esDeployment == null){esDeployment=deployment;} 
				esDeployment.setPendingStartTS(simpleDateFormat.format(new Date()));
				esDeployment.setDeploymentState(deployment.getDeploymentState());
			}
			else if(SearchConstants.DPMT_STATE_PAUSED.equalsIgnoreCase(deployment.getDeploymentState())){
				esDeployment.setPauseCnt(deployment.getPauseCnt() + 1);
				esDeployment.setPausedStartTS(simpleDateFormat.format(new Date()));
				if(SearchConstants.DPMT_STATE_ACTIVE.equalsIgnoreCase(esDeployment.getDeploymentState())){
					esDeployment.setActiveEndTS(simpleDateFormat.format(new Date()));

					double activeDuration = esDeployment.getActiveDuration() +
							((( simpleDateFormat.parse(esDeployment.getActiveEndTS()).getTime()) - ( simpleDateFormat.parse(esDeployment.getActiveStartTS()).getTime())) / 1000.0);
					esDeployment.setActiveDuration(Math.round(activeDuration * 1000.0) / 1000.0);
				}
				esDeployment.setDeploymentState(deployment.getDeploymentState());
				updateTotalTime(esDeployment);
			}	
			else if(SearchConstants.DPMT_STATE_FAILED.equalsIgnoreCase(deployment.getDeploymentState())){
				esDeployment.setFailureCnt(esDeployment.getFailureCnt() + 1);
				esDeployment.setFailedStartTS(simpleDateFormat.format(new Date()));
				if(SearchConstants.DPMT_STATE_ACTIVE.equalsIgnoreCase(esDeployment.getDeploymentState())){
					esDeployment.setActiveEndTS(simpleDateFormat.format(new Date()));

					double activeDuration = esDeployment.getActiveDuration() +
							((( simpleDateFormat.parse(esDeployment.getActiveEndTS()).getTime()) - simpleDateFormat.parse(esDeployment.getActiveStartTS()).getTime()) / 1000.0);
					esDeployment.setActiveDuration(Math.round(activeDuration * 1000.0) / 1000.0);
				}
				esDeployment.setDeploymentState(deployment.getDeploymentState());
				updateTotalTime(esDeployment);
			}
		} catch (Exception e) {
			logger.error("Error in processing deployment message "+ e.getMessage());
		}
		
		return esDeployment!=null?esDeployment:deployment;
	}
	
	private CmsDeploymentSearch fetchDeploymentRecord(long deploymentId) {
		SearchQuery searchQuery = new NativeSearchQueryBuilder()        // todo: index name
				.withTypes("deployment").withQuery(queryString(String.valueOf(deploymentId)).field("deploymentId"))
				.build();
		
		List<CmsDeploymentSearch> esDeploymentList = template.queryForList(searchQuery, CmsDeploymentSearch.class);
		if(esDeploymentList.size() > 1){
           cleanOldDeployment(esDeploymentList.get(0).getDeploymentId());
		}
		return !esDeploymentList.isEmpty()?esDeploymentList.get(0):null;
	}


	/**
	 * Update the total time taken by the deployment before it reaches a terminal state
	 * 
	 * @param esDeployment
	 */
	private void updateTotalTime(CmsDeploymentSearch esDeployment){
		double tt = esDeployment.getActiveDuration() + esDeployment.getFailedDuration() + esDeployment.getPausedDuration() + esDeployment.getPendingDuration();
		 esDeployment.setTotalTime(Math.round(tt * 1000.0) / 1000.0);
	}


	private boolean isFinalState(String state){
		return SearchConstants.DPMT_STATE_COMPLETE.equalsIgnoreCase(state) || SearchConstants.DPMT_STATE_CANCELED.equalsIgnoreCase(state);
	}


	/**
	 * Handles the edge case condition where deployments can spawn to multiple weeks.
	   Cleans duplicate deployments from deployment indices as cms indices are created weekly.
	 * @param deploymentId
     */
	private void cleanOldDeployment(Long deploymentId){

		SearchResponse response = client.prepareSearch("cms")
				.setTypes("deployment")
				.setQuery(queryString(String.valueOf(deploymentId)).field("deploymentId"))
				.execute()
				.actionGet();

		//Skip the latest week deployment id and deletes all others
		Arrays.stream(response.getHits().getHits())
				.sorted((hit1 , hit2) -> (hit1.getIndex().compareTo(hit2.getIndex()))* -1)
				.skip(1).forEach(hit -> {
			template.delete(hit.getIndex(),"deployment",hit.getId());
			logger.info("Deleted duplicate deployment " + hit.getId() + " in index " + hit.getIndex());
		});


	}
	

	public ElasticsearchTemplate getTemplate() {
		return template;
	}

	public void setTemplate(ElasticsearchTemplate template) {
		this.template = template;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}
}
