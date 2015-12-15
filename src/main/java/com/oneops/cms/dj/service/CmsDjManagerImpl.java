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
package com.oneops.cms.dj.service;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.oneops.cms.dj.domain.CmsDeployment;
import com.oneops.cms.dj.domain.CmsDpmtApproval;
import com.oneops.cms.dj.domain.CmsDpmtRecord;
import com.oneops.cms.dj.domain.CmsDpmtStateChangeEvent;
import com.oneops.cms.dj.domain.CmsRelease;
import com.oneops.cms.dj.domain.CmsRfcCI;
import com.oneops.cms.dj.domain.CmsRfcRelation;
import com.oneops.cms.dj.domain.CmsWorkOrder;
import com.oneops.cms.exceptions.DJException;
import com.oneops.cms.util.CmsError;

/**
 * The Class CmsDjManagerImpl.
 */
public class CmsDjManagerImpl implements CmsDjManager {
	static Logger logger = Logger.getLogger(CmsDjManagerImpl.class);
	private CmsRfcProcessor rfcProcessor;
	private CmsDpmtProcessor dpmtProcessor;
//	private CmsWoProvider woProvider;
	
	/**
	 * Sets the rfc processor.
	 *
	 * @param rfcProcessor the new rfc processor
	 */
	public void setRfcProcessor(CmsRfcProcessor rfcProcessor) {
		this.rfcProcessor = rfcProcessor;
	}

	/**
	 * Sets the dpmt processor.
	 *
	 * @param dpmtProcessor the new dpmt processor
	 */
	public void setDpmtProcessor(CmsDpmtProcessor dpmtProcessor) {
		this.dpmtProcessor = dpmtProcessor;
	}
	
	/**
	 * Sets the wo provider.
	 *
	 * @param woProvider the new wo provider
//	 */
//	public void setWoProvider(CmsWoProvider woProvider) {
//		this.woProvider = woProvider;
//	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#updateRelease(com.oneops.cms.dj.domain.CmsRelease)
	 */
	@Override
	public CmsRelease updateRelease(CmsRelease release) {
		return rfcProcessor.updateRelease(release);
	}
	
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#createRelease(com.oneops.cms.dj.domain.CmsRelease)
	 */
	@Override
	public CmsRelease createRelease(CmsRelease release) {
		if (rfcProcessor.getReleaseBy3(release.getNsPath(), null, "open").size() > 0 ) {
			String err = "There is open release for this namespace!";
			logger.error(err);
			throw new DJException(CmsError.DJ_OPEN_RELEASE_FOR_NAMESPACE_ERROR, err);
		}
		return rfcProcessor.createRelease(release);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#commitRelease(long, java.lang.Boolean, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void commitRelease(long releaseId,Boolean setDfValue, String newCiState, String userId, String desc) {
		rfcProcessor.commitRelease(releaseId, setDfValue, newCiState, userId, desc);
	}

	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getReleaseById(long)
	 */
	@Override
	public CmsRelease getReleaseById(long releaseId) {
		return rfcProcessor.getReleaseById(releaseId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getReleaseBy3(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public List<CmsRelease> getReleaseBy3(String nsPath, String releaseName,
			String releaseState) {
		return rfcProcessor.getReleaseBy3(nsPath, releaseName, releaseState);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getLatestRelease(java.lang.String, java.lang.String)
	 */
	@Override
	public List<CmsRelease> getLatestRelease(String nsPath, String releaseState) {
		return rfcProcessor.getLatestRelease(nsPath, releaseState);
	}
	
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#deleteRelease(long)
	 */
	@Override
	public long deleteRelease(long releaseId) {
		return rfcProcessor.deleteRelease(releaseId);
	}
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#createRfcCI(com.oneops.cms.dj.domain.CmsRfcCI)
	 */
	@Override
	public CmsRfcCI createRfcCI(CmsRfcCI rfcCi) {
		long newRfcId = rfcProcessor.createRfcCI(rfcCi); 
		return rfcProcessor.getRfcCIById(newRfcId);
	}
	
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#updateRfcCI(com.oneops.cms.dj.domain.CmsRfcCI)
	 */
	@Override
	public CmsRfcCI updateRfcCI(CmsRfcCI rfcCi) {
		long newRfcId = rfcProcessor.updateRfcCI(rfcCi);
		return rfcProcessor.getRfcCIById(newRfcId);
	}
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#rmRfcCiFromRelease(long)
	 */
	@Override
	public long rmRfcCiFromRelease(long rfcId) {
		return rfcProcessor.rmRfcCiFromRelease(rfcId);
	}
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getRfcCIById(long)
	 */
	@Override
	public CmsRfcCI getRfcCIById(long rfcId) {
		return rfcProcessor.getRfcCIById(rfcId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getRfcCIBy3(long, java.lang.Boolean, java.lang.Long)
	 */
	@Override
	public List<CmsRfcCI> getRfcCIBy3(long releaseId, Boolean isActive, Long ciId) {
		return rfcProcessor.getRfcCIBy3(releaseId, isActive, ciId);
	}

    /* (non-Javadoc)
     * @see com.oneops.cms.dj.service.CmsDjManager#getClosedRfcCIByCiId(long)
     */
    @Override
    public List<CmsRfcCI> getClosedRfcCIByCiId(long ciId) {
        return rfcProcessor.getClosedRfcCIByCiId(ciId);
    }

    /* (non-Javadoc)
     * @see com.oneops.cms.dj.service.CmsDjManager#getRollUpRfc(long, long)
     */
    @Override
    public CmsRfcCI getRollUpRfc(long ciId, long rfcId) {
        return rfcProcessor.getRollUpRfc( ciId, rfcId);
    }

    /* (non-Javadoc)
     * @see com.oneops.cms.dj.service.CmsDjManager#createRfcRelation(com.oneops.cms.dj.domain.CmsRfcRelation)
     */
    @Override
	public CmsRfcRelation createRfcRelation(CmsRfcRelation rel) {
		long newRfcId = rfcProcessor.createRfcRelation(rel); 
		return rfcProcessor.getRfcRelationById(newRfcId);
	}	

	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#rmRfcRelationFromRelease(long)
	 */
	@Override
	public long rmRfcRelationFromRelease(long rfcId) {
		return rfcProcessor.rmRfcRelationFromRelease(rfcId);
	}
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getRfcRelationById(long)
	 */
	@Override
	public CmsRfcRelation getRfcRelationById(long rfcId) {
		return rfcProcessor.getRfcRelationById(rfcId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getRfcRelationByReleaseId(long)
	 */
	@Override
	public List<CmsRfcRelation> getRfcRelationByReleaseId(long releaseId) {
		return rfcProcessor.getRfcRelationByReleaseId(releaseId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getRfcRelationBy3(long, java.lang.Boolean, java.lang.Long, java.lang.Long)
	 */
	@Override
	public List<CmsRfcRelation> getRfcRelationBy3(long releaseId,
			Boolean isActive, Long fromCiId, Long toCiId) {
		return rfcProcessor.getRfcRelationBy4(releaseId, isActive, fromCiId, toCiId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#updateRfcRelation(com.oneops.cms.dj.domain.CmsRfcRelation)
	 */
	@Override
	public CmsRfcRelation updateRfcRelation(CmsRfcRelation rfcRelation) {
		long newRfcId = rfcProcessor.updateRfcRelation(rfcRelation);
		return rfcProcessor.getRfcRelationById(newRfcId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#createDeployment(com.oneops.cms.dj.domain.CmsDeployment)
	 */
	@Override
	public CmsDeployment createDeployment(CmsDeployment dpmt) {
		return dpmtProcessor.deployRelease(dpmt);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#updateDeployment(com.oneops.cms.dj.domain.CmsDeployment)
	 */
	@Override
	public CmsDeployment updateDeployment(CmsDeployment dpmt) {
		return dpmtProcessor.updateDeployment(dpmt);
	}
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#deployRelease(long)
	 */
	@Override
	public CmsDeployment deployRelease(long releaseId) {
		CmsDeployment dpmt = new CmsDeployment();
		dpmt.setReleaseId(releaseId);
		return dpmtProcessor.deployRelease(dpmt);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getDeployment(long)
	 */
	@Override
	public CmsDeployment getDeployment(long dpmtId) {
		return dpmtProcessor.getDeployment(dpmtId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#findDeployment(java.lang.String, java.lang.String, java.lang.Boolean, boolean)
	 */
	@Override
	public List<CmsDeployment> findDeployment(String nsPath, String state, Boolean recursive, boolean latest) {
		if (latest) {
			return dpmtProcessor.findLatestDeployment(nsPath, state, recursive);
		} else {
			return dpmtProcessor.findDeployment(nsPath, state, recursive);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#countDeployments(java.lang.String, java.lang.String, java.lang.Boolean)
	 */
	@Override
	public long countDeployments(String nsPath, String state,
			Boolean recursive) {
		return dpmtProcessor.countDeployment(nsPath, state, recursive);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#countDeploymentGroupByNsPath(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<String, Long> countDeploymentGroupByNsPath(String nsPath,
			String state) {
		return dpmtProcessor.countDeploymentGroupBy(nsPath, state);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#findDeploymentByReleaseId(long, java.lang.String, boolean)
	 */
	@Override
	public List<CmsDeployment> findDeploymentByReleaseId(long releaseId, String state, boolean latest) {
		if (latest) {
			return dpmtProcessor.findLatestDeploymentByReleaseId(releaseId, state);
		} else {
			return dpmtProcessor.findDeploymentByReleaseId(releaseId, state);
		}
	}
	

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getDpmtRecords(long)
	 */
	@Override
	public List<CmsDpmtRecord> getDpmtRecords(long dpmtId) {
		return dpmtProcessor.getDeploymentRecords(dpmtId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getDpmtRecordCis(long)
	 */
	@Override
	public List<CmsDpmtRecord> getDpmtRecordCis(long dpmtId) {
		return dpmtProcessor.getDeploymentRecordCis(dpmtId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getDpmtRecordRelations(long)
	 */
	@Override
	public List<CmsDpmtRecord> getDpmtRecordRelations(long dpmtId) {
		return dpmtProcessor.getDeploymentRecordRelations(dpmtId);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getWorkOrders(long, java.lang.String, java.lang.Integer)
	 */
//	@Override
//	public List<CmsWorkOrder> getWorkOrders(long deploymentId, String state,
//			Integer execOrder, Integer limit) {
//		return woProvider.getWorkOrders(deploymentId, state, execOrder, limit);
//	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getWorkOrderIds(long, java.lang.String, java.lang.Integer,java.lang.Integer)
	 */
//	@Override
//	public List<CmsWorkOrder> getWorkOrderIds(long deploymentId, String state,
//			Integer execOrder, Integer limit) {
//		return woProvider.getWorkOrderIds(deploymentId, state, execOrder, limit);
//	}
//
//	/* (non-Javadoc)
//	 * @see com.oneops.cms.dj.service.CmsDjManager#getWorkOrder(long, java.lang.String, java.lang.Integer)
//	 */
//	public CmsWorkOrder getWorkOrder(long dpmtRecordId, String state, Integer execOrder) {
//		return woProvider.getWorkOrder(dpmtRecordId, state, execOrder);
//	}
	
	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#updateDpmtRecord(com.oneops.cms.dj.domain.CmsDpmtRecord)
	 */
	@Override
	public CmsDpmtRecord updateDpmtRecord(CmsDpmtRecord rec) {
		return dpmtProcessor.updateDpmtRecord(rec);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#completeWorkOrder(com.oneops.cms.dj.domain.CmsWorkOrder)
	 */
	@Override
	public void completeWorkOrder(CmsWorkOrder wo) {
		dpmtProcessor.completeWorkOrder(wo);
	}

	/* (non-Javadoc)
	 * @see com.oneops.cms.dj.service.CmsDjManager#getDpmtRecordCis()
	 */
	@Override
	public List<CmsDpmtRecord> getDpmtRecordCis(long dpmtId, String state,
			Integer execOrder) {
		return dpmtProcessor.getDeploymentRecordCisByState(dpmtId, state, execOrder);
	}

	@Override
	public List<CmsDpmtRecord> getDeploymentRecordByCiId(long ciId, String state) {
		return dpmtProcessor.getDeploymentRecordByCiId(ciId, state);
	}

	@Override
	public CmsDpmtRecord getDpmtRecord(long dpmtRecordId) {
		return dpmtProcessor.getDeploymentRecord(dpmtRecordId);
	}

	@Override
	public long getDeploymentRecordCount(long dpmtId, String state,
			Integer execOrder) {
		return dpmtProcessor.getDeploymentRecordCount(dpmtId, state, execOrder);
	}

	@Override
	public List<CmsDpmtStateChangeEvent> getDeploymentStateHist(
			long deploymentId) {
		return dpmtProcessor.getDeploymentStateHist(deploymentId);
	}

	/*
	@Override
	public List<CmsDeployment> dpmtApprove(List<CmsDpmtApproval> approvals) {
		return dpmtProcessor.dpmtApprove(approvals);
	}
	*/
	
	@Override
	public List<CmsDpmtApproval> getDeploymentApprovals(long dpmtId) {
		return dpmtProcessor.getDeploymentApprovals(dpmtId);
	}

	@Override
	public CmsDpmtApproval getDeploymentApproval(long approvalId) {
		return dpmtProcessor.getDeploymentApproval(approvalId);
	}

	@Override
	public List<CmsDpmtApproval> updateApprovalList(List<CmsDpmtApproval> approvals) {
		return dpmtProcessor.updateApprovalList(approvals);
	}
	
}
