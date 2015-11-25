package com.oneops.search.msg.processor;

import static org.elasticsearch.index.query.QueryBuilders.queryString;

import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import com.oneops.cms.cm.ops.domain.OpsProcedureState;
import com.oneops.cms.util.CmsConstants;
import com.oneops.search.domain.CmsOpsProcedureSearch;
import com.oneops.search.util.SearchUtil;

public class OpsProcMessageProcessor {

	private static Logger logger = Logger.getLogger(OpsProcMessageProcessor.class);
	private ElasticsearchTemplate template;
	
	
	/**
	 * 
	 * @param procedure
	 * @return
	 */
	public CmsOpsProcedureSearch processOpsProcMsg(CmsOpsProcedureSearch procedure) {
		
		CmsOpsProcedureSearch esProcedure = null;
		try {
			esProcedure = fetchprocedureRecord(procedure.getProcedureId());
			
			if(isFinalState(procedure.getProcedureState().getName())){
				
				if(esProcedure!=null && OpsProcedureState.canceled.getName().equalsIgnoreCase(procedure.getProcedureState().getName())) {
					if(OpsProcedureState.failed.getName().equalsIgnoreCase(esProcedure.getProcedureState().getName())){
						esProcedure.setFailedEndTS(DateUtil.formatDate(new Date(), CmsConstants.SEARCH_TS_PATTERN));
						double failedDuration = esProcedure.getFailedDuration() +
								(((SearchUtil.getTimefromDate(esProcedure.getFailedEndTS())) - (SearchUtil.getTimefromDate(esProcedure.getFailedStartTS())))/1000.0); 
						esProcedure.setFailedDuration(Math.round(failedDuration * 1000.0)/1000.0);
					}
				}else if(esProcedure!=null && OpsProcedureState.complete.getName().equalsIgnoreCase(procedure.getProcedureState().getName())){
					if(OpsProcedureState.active.getName().equalsIgnoreCase(esProcedure.getProcedureState().getName())){
						esProcedure.setActiveEndTS(DateUtil.formatDate(new Date(), CmsConstants.SEARCH_TS_PATTERN));
						double activeDuration = esProcedure.getActiveDuration() +
								(((SearchUtil.getTimefromDate((esProcedure.getActiveEndTS()))) - (SearchUtil.getTimefromDate(esProcedure.getActiveStartTS()))) / 1000.0);
						esProcedure.setActiveDuration(Math.round(activeDuration * 1000.0)/1000.0);
					}
				}
				esProcedure.setProcedureState(procedure.getProcedureState());
				esProcedure.setTotalTime(((System.currentTimeMillis()) - (esProcedure.getCreated().getTime()))/1000.0);
			}
			else if(OpsProcedureState.active.getName().equalsIgnoreCase(procedure.getProcedureState().getName())){

				if(esProcedure!=null){ 
					esProcedure.setActiveStartTS(DateUtil.formatDate(new Date(), CmsConstants.SEARCH_TS_PATTERN));
					if(OpsProcedureState.failed.getName().equalsIgnoreCase(esProcedure.getProcedureState().getName())){
						esProcedure.setRetryCount(esProcedure.getRetryCount()+1);
						esProcedure.setFailedEndTS(DateUtil.formatDate(new Date(), CmsConstants.SEARCH_TS_PATTERN));
						double failedDuration = esProcedure.getFailedDuration() +
								(((SearchUtil.getTimefromDate(esProcedure.getFailedEndTS())) - (SearchUtil.getTimefromDate(esProcedure.getFailedStartTS())))/1000.0);
						esProcedure.setFailedDuration(Math.round(failedDuration * 1000.0)/1000.0);
						esProcedure.setProcedureState(procedure.getProcedureState());
					}
				}
				else{
					procedure.setActiveStartTS(DateUtil.formatDate(new Date(), CmsConstants.SEARCH_TS_PATTERN));
				}
			}
			else if(OpsProcedureState.failed.getName().equalsIgnoreCase(procedure.getProcedureState().getName())){
				esProcedure.setFailureCnt(esProcedure.getFailureCnt() + 1);
				esProcedure.setFailedStartTS(DateUtil.formatDate(new Date(), CmsConstants.SEARCH_TS_PATTERN));
				if(OpsProcedureState.active.getName().equalsIgnoreCase(esProcedure.getProcedureState().getName())){
					esProcedure.setActiveEndTS(DateUtil.formatDate(new Date(), CmsConstants.SEARCH_TS_PATTERN));
					double activeDuration = esProcedure.getActiveDuration() +
							(((SearchUtil.getTimefromDate((esProcedure.getActiveEndTS()))) - (SearchUtil.getTimefromDate(esProcedure.getActiveStartTS()))) / 1000.0);
					esProcedure.setActiveDuration(Math.round(activeDuration * 1000.0)/1000.0);
				}
				esProcedure.setProcedureState(procedure.getProcedureState());
			}
		} catch (Exception e) {
			logger.error("Error in processing ops-procedure message "+ e.getMessage());
		}
		
		return esProcedure!=null?esProcedure:procedure;
	}
	
	
	private CmsOpsProcedureSearch fetchprocedureRecord(long procedureId) {
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
        .withTypes("opsprocedure").withQuery(queryString(String.valueOf(procedureId)).field("procedureId"))
        .build();
		
		List<CmsOpsProcedureSearch> esProcedureList = template.queryForList(searchQuery, CmsOpsProcedureSearch.class);
		return !esProcedureList.isEmpty()?esProcedureList.get(0):null;
	}


	private boolean isFinalState(String state){
		return OpsProcedureState.complete.getName().equalsIgnoreCase(state) || OpsProcedureState.canceled.getName().equalsIgnoreCase(state);
	}
	

	public ElasticsearchTemplate getTemplate() {
		return template;
	}

	public void setTemplate(ElasticsearchTemplate template) {
		this.template = template;
	}
}
