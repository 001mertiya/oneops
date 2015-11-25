package com.oneops.transistor.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.oneops.cms.cm.domain.CmsCI;
import com.oneops.cms.cm.domain.CmsCIRelation;
import com.oneops.cms.cm.service.CmsCmProcessor;
import com.oneops.cms.dj.domain.CmsDeployment;
import com.oneops.cms.dj.domain.CmsRelease;
import com.oneops.cms.dj.service.CmsDpmtProcessor;
import com.oneops.cms.dj.service.CmsRfcProcessor;
import com.oneops.cms.util.CmsConstants;
import com.oneops.cms.util.CmsError;
import com.oneops.cms.util.CmsUtil;
import com.oneops.transistor.exceptions.TransistorException;

public class BomManagerImpl implements BomManager {

	static Logger logger = Logger.getLogger(BomManagerImpl.class);

	/*
	private static Comparator<CmsCIRelation> BINDING_COMPARATOR = new Comparator<CmsCIRelation>()
    {
        public int compare(CmsCIRelation binding1, CmsCIRelation binding2)
        {
            return Integer.valueOf(binding1.getAttribute("priority").getDjValue()) - Integer.valueOf(binding2.getAttribute("priority").getDjValue());
        }
    };
	
	private static Comparator<CmsCIRelation> REVERSE_BINDING_COMPARATOR = new Comparator<CmsCIRelation>()
		    {
		        public int compare(CmsCIRelation binding1, CmsCIRelation binding2)
		        {
		            return Integer.valueOf(binding1.getAttribute("priority").getDjValue()) - Integer.valueOf(binding2.getAttribute("priority").getDjValue());
		        }
		    };
	*/	    

    private CmsCmProcessor cmProcessor;
	private CmsRfcProcessor rfcProcessor;
	private BomRfcBulkProcessor bomRfcProcessor;
	private TransUtil trUtil;
	private CmsDpmtProcessor dpmtProcessor;
	private CmsUtil cmsUtil;
	
	public void setCmsUtil(CmsUtil cmsUtil) {
		this.cmsUtil = cmsUtil;
	}

	public void setTrUtil(TransUtil trUtil) {
		this.trUtil = trUtil;
	}

	public void setCmProcessor(CmsCmProcessor cmProcessor) {
		this.cmProcessor = cmProcessor;
	}

	public void setRfcProcessor(CmsRfcProcessor rfcProcessor) {
		this.rfcProcessor = rfcProcessor;
	}

	public void setBomRfcProcessor(BomRfcBulkProcessor bomRfcProcessor) {
		this.bomRfcProcessor = bomRfcProcessor;
	}
	
	public void setDpmtProcessor(CmsDpmtProcessor dpmtProcessor) {
		this.dpmtProcessor = dpmtProcessor;
	}

	@Override
	public long generateAndDeployBom(long envId, String userId, Set<Long> excludePlats, String desc, boolean commit) {
		long releaseId = generateBom(envId, userId, excludePlats, desc, commit);
		if (releaseId > 0) {
			return submitDeployment(releaseId, userId);
		} else {
			return 0;
		}
		
	}	
	

	@Override
	public long generateBom(long envId, String userId, Set<Long> excludePlats, String desc, boolean commit) {
		return generateBomForClouds(envId, userId, excludePlats, desc, commit);
	}
		
	public long generateBomForClouds(long envId, String userId, Set<Long> excludePlats, String desc, boolean commit) {

		long startTime = System.currentTimeMillis();

		CmsCI env = cmProcessor.getCiById(envId);
		
		String manifestNs = env.getNsPath() + "/" + env.getCiName() + "/manifest";
		String bomNsPath = env.getNsPath() + "/" + env.getCiName() + "/bom";
		
		check4openDeployment(bomNsPath);

		trUtil.verifyAndCreateNS(bomNsPath);
		
		trUtil.lockNS(bomNsPath);
		if (commit) {
			//get open manifest release and soft commit it (no real deletes)
			commitManifestRelease(manifestNs, bomNsPath, userId, desc);
		}
		
		Map<String,String> envVars = cmsUtil.getGlobalVars(env);
		
		logger.info(">>> Starting generating BOM for active clouds... ");
		int execOrder = generateBomForActiveClouds(envId, userId, excludePlats, manifestNs, bomNsPath, envVars, desc);

		logger.info(">>> Starting generating BOM for offline clouds... ");
		execOrder = generateBomForOfflineClouds(envId, userId, excludePlats, manifestNs, bomNsPath, envVars, execOrder, desc);
		//logger.info(">>>> execOrder=" + execOrder);
		
		long relelaseId = getPopulateParentAndGetReleaseId(bomNsPath, manifestNs);
		long rfcCount = 0;
		if (relelaseId >0) {
			rfcProcessor.brushExecOrder(relelaseId);
			rfcCount = rfcProcessor.getRfcCount(relelaseId);
		}
		
		long timeTook = System.currentTimeMillis() - startTime;
		logger.info(bomNsPath + " >>> Time to process Bom " + timeTook + " ms. RFCs created = " + rfcCount);
		
		//if release id is 0 check if there is global var in pending_deletion state. If yes delete it.
		if(relelaseId == 0){
			for (CmsCI localVar : cmProcessor.getCiByNsLikeByStateNaked(manifestNs, "manifest.Globalvar", "pending_deletion")) {
				cmProcessor.deleteCI(localVar.getCiId(), true);
			}
		}
		
		return relelaseId;
		
	}
	
	public int generateBomForActiveClouds(long envId, String userId, Set<Long> excludePlats, String manifestNs, String bomNsPath, Map<String,String> envVars, String desc) {
		
		long globalStartTime = System.currentTimeMillis();
		CmsCI env = cmProcessor.getCiById(envId);
		logger.info(manifestNs + " >>> Starting processing environemt " + env.getCiName());
		List<CmsCIRelation> platRels = cmProcessor.getFromCIRelations(envId, null, "ComposedOf", "manifest.Platform");
		
		Set<Long> disabledPlats = new HashSet<Long>();
		for (CmsCIRelation comOf : platRels) {
			if (comOf.getAttribute("enabled") != null 
				&& comOf.getAttribute("enabled").getDjValue().equalsIgnoreCase("false")) {
				disabledPlats.add(comOf.getToCiId());
			}
		}

		Map<Integer, List<CmsCI>> platsToProcess = getOrderedPlatforms(platRels, disabledPlats);
		
		int maxOrder = 0;
		for (Integer order : platsToProcess.keySet()) {
			maxOrder = (order > maxOrder) ? order : maxOrder;
		}
		
		int startingExecOrder = 1;
		
		for (int i = 1; i<=maxOrder ; i++) {
			if (platsToProcess.containsKey(i)) {
				startingExecOrder = (startingExecOrder >1 ) ? startingExecOrder+1 : startingExecOrder;
				int stepMaxOrder = 0;
				for (CmsCI plat : platsToProcess.get(i)) {
					//if this palt is in exclude list don't touch it
					if (excludePlats != null && excludePlats.contains(plat.getCiId())) {
						continue;
					}	
					long platStartTime = System.currentTimeMillis();
					//now we need to check if the cloud is active for this given platform
					List<CmsCIRelation> platformCloudRels = cmProcessor.getFromCIRelations(plat.getCiId(), "base.Consumes", "account.Cloud");
					
					if (platformCloudRels.size() >0) {
						
						//Collections.sort(platformCloudRels,BINDING_COMPARATOR);
						
						int platExecOrder = startingExecOrder;
						int thisPlatMaxExecOrder = 0;
						SortedMap<Integer, SortedMap<Integer, List<CmsCIRelation>>> orderedClouds = getOrderedClouds(platformCloudRels, false);
						for (SortedMap<Integer, List<CmsCIRelation>> priorityClouds : orderedClouds.values()) {
							for (List<CmsCIRelation> orderCloud : priorityClouds.values()) {
								for (CmsCIRelation platformCloudRel : orderCloud) {
									if (platformCloudRel.getAttribute("adminstatus") != null
										&& !CmsConstants.CLOUD_STATE_ACTIVE.equals(platformCloudRel.getAttribute("adminstatus").getDjValue())) {
										continue;
									} 

									Map<String,String> cloudVars = cmsUtil.getCloudVars(platformCloudRel.getToCi());

									int maxExecOrder = 0;
									if (disabledPlats.contains(plat.getCiId())
										|| plat.getCiState().equalsIgnoreCase("pending_deletion")) {
										maxExecOrder = bomRfcProcessor.deleteManifestPlatform(plat, platformCloudRel, bomNsPath, platExecOrder, userId);
									} else {
										maxExecOrder = bomRfcProcessor.processManifestPlatform(plat, platformCloudRel, bomNsPath, platExecOrder, envVars, cloudVars, userId, true, true);
									}
									stepMaxOrder = (maxExecOrder > stepMaxOrder) ? maxExecOrder : stepMaxOrder;
									thisPlatMaxExecOrder = (maxExecOrder > thisPlatMaxExecOrder) ? maxExecOrder : thisPlatMaxExecOrder; 
								}
								platExecOrder = (thisPlatMaxExecOrder > platExecOrder) ? thisPlatMaxExecOrder + 1 : platExecOrder;
							}
						}
					} else {
						//if platform does not have a relation to the cloud - consider it disabled
						continue;
					}
					logger.info(plat.getNsPath() + " >>> Done processing platform " + plat.getCiName() + "for all clouds, time spent - " + (System.currentTimeMillis() - platStartTime));
				}
				startingExecOrder = (stepMaxOrder >0 ) ? stepMaxOrder+1 : startingExecOrder;
			}
		}
		
		logger.info(manifestNs + " >>> Done processing environemt " + env.getCiName() + ", time spent - " + (System.currentTimeMillis() - globalStartTime));

		return startingExecOrder;
	}

	
	
	public int generateBomForOfflineClouds(long envId, String userId, Set<Long> excludePlats, String manifestNs, String bomNsPath, Map<String,String> envVars, int startingExecOrder, String desc) {
		
		long globalStartTime = System.currentTimeMillis();
		
		CmsCI env = cmProcessor.getCiById(envId);
		logger.info(manifestNs + " >>> Starting processing environemt " + env.getCiName());
		
		List<CmsCIRelation> platRels = cmProcessor.getFromCIRelations(envId, null, "ComposedOf", "manifest.Platform");
		
		Set<Long> disabledPlats = new HashSet<Long>();
		for (CmsCIRelation comOf : platRels) {
			disabledPlats.add(comOf.getToCiId());
		}

		Map<Integer, List<CmsCI>> platsToProcess = getOrderedPlatforms(platRels, disabledPlats);
		
		int maxOrder = 0;
		for (Integer order : platsToProcess.keySet()) {
			maxOrder = (order > maxOrder) ? order : maxOrder;
		}
		
		for (int i = 1; i<=maxOrder ; i++) {
			if (platsToProcess.containsKey(i)) {
				startingExecOrder = (startingExecOrder >1 ) ? startingExecOrder+1 : startingExecOrder;
				int stepMaxOrder = 0;
				for (CmsCI plat : platsToProcess.get(i)) {
					//if this palt is in exclude list don't touch it
					if (excludePlats != null && excludePlats.contains(plat.getCiId())) {
						continue;
					}	
					
					//now we need to check if the cloud is active for this given platform
					List<CmsCIRelation> platformCloudRels = cmProcessor.getFromCIRelations(plat.getCiId(), "base.Consumes", "account.Cloud");
					
					if (platformCloudRels.size() >0) {
						int platExecOrder = startingExecOrder;
						SortedMap<Integer, SortedMap<Integer, List<CmsCIRelation>>> orderedClouds = getOrderedClouds(platformCloudRels, true);
						for (SortedMap<Integer, List<CmsCIRelation>> priorityClouds : orderedClouds.values()) {
							for (List<CmsCIRelation> orderCloud : priorityClouds.values()) {
								for (CmsCIRelation platformCloudRel : orderCloud) {
									if (!CmsConstants.CLOUD_STATE_OFFLINE.equals(platformCloudRel.getAttribute("adminstatus").getDjValue())) {
										continue;
									} 

									int maxExecOrder = bomRfcProcessor.deleteManifestPlatform(plat, platformCloudRel, bomNsPath, platExecOrder, userId);
									stepMaxOrder = (maxExecOrder > stepMaxOrder) ? maxExecOrder : stepMaxOrder;
								}
								platExecOrder = (stepMaxOrder > platExecOrder) ? stepMaxOrder + 1 : platExecOrder;
							}
						}
					} else {
						//if platform does not have a relation to the cloud - consider it disabled
						continue;
					}
				}
				startingExecOrder = (stepMaxOrder >0 ) ? stepMaxOrder+1 : startingExecOrder;
			}
		}

		logger.info(manifestNs + " >>> Done processing environemt " + env.getCiName() + " offline clouds, time spent - " + (System.currentTimeMillis() - globalStartTime));

		return startingExecOrder;
	}
	
	private SortedMap<Integer, SortedMap<Integer, List<CmsCIRelation>>> getOrderedClouds(List<CmsCIRelation> cloudRels, boolean reverse) {
		
		SortedMap<Integer, SortedMap<Integer, List<CmsCIRelation>>> result = reverse ? 
					new TreeMap<Integer, SortedMap<Integer, List<CmsCIRelation>>>(Collections.reverseOrder())
					: new TreeMap<Integer, SortedMap<Integer, List<CmsCIRelation>>>();
		
		for (CmsCIRelation binding : cloudRels) {
		
			Integer priority = Integer.valueOf(binding.getAttribute("priority").getDjValue());
			Integer order = 1;
			if (binding.getAttributes().containsKey("dpmt_order")) {
				order = Integer.valueOf(binding.getAttribute("dpmt_order").getDjValue());
			}
			if (!result.containsKey(priority)) {
				result.put(priority, new TreeMap<Integer, List<CmsCIRelation>>());
			}
			if (!result.get(priority).containsKey(order)) {
				result.get(priority).put(order, new ArrayList<CmsCIRelation>());
			}
			result.get(priority).get(order).add(binding);
		}
		
		return result;
	}
	
	
	private long submitDeployment(long releaseId, String userId){

		CmsRelease bomRelease = rfcProcessor.getReleaseById(releaseId);
		CmsDeployment dpmt = new CmsDeployment();
		dpmt.setNsPath(bomRelease.getNsPath());
		dpmt.setReleaseId(bomRelease.getReleaseId());
		dpmt.setCreatedBy(userId);
		CmsDeployment newDpmt = dpmtProcessor.deployRelease(dpmt); 
		logger.info("created new deployment - " + newDpmt.getDeploymentId());
		return newDpmt.getDeploymentId();
	}

	/*
	private boolean hasActive(List<CmsCIRelation> rels, Set<Long> disabledPlats) {
		for (CmsCIRelation rel : rels) {
			if (!"pending_deletion".equalsIgnoreCase(rel.getRelationState()) && !disabledPlats.contains(rel.getFromCiId())) {
				return true; 
			}
		}
		return false;
	}
	*/
	
	private long getPopulateParentAndGetReleaseId(String nsPath, String manifestNsPath) {
		List<CmsRelease> releases = rfcProcessor.getLatestRelease(nsPath, "open"); 
		if (releases.size() >0) {
			CmsRelease bomRelease = releases.get(0);
			List<CmsRelease> manifestReleases = rfcProcessor.getLatestRelease(manifestNsPath, "closed");
			if (manifestReleases.size()>0) bomRelease.setParentReleaseId(manifestReleases.get(0).getReleaseId()); 
			rfcProcessor.updateRelease(bomRelease);
			return bomRelease.getReleaseId(); 
		}
		return 0;
	}
	
	private Map<Integer, List<CmsCI>> getOrderedPlatforms(List<CmsCIRelation> platRels, Set<Long> disabledPlats) {

		Map<Long, Integer> plat2ExecOrderMap = new HashMap<Long, Integer>();
		Map<Long, CmsCI> plats = new HashMap<Long, CmsCI>();
		for (CmsCIRelation platRel : platRels) {
			plats.put(platRel.getToCiId(), platRel.getToCi());
			List<CmsCIRelation> linksToRels = cmProcessor.getFromCIRelationsNaked(platRel.getToCiId(), "manifest.LinksTo", "manifest.Platform");
			if (linksToRels.size()==0) {
				plat2ExecOrderMap.put(platRel.getToCiId(), 1);
				processPlatformsOrder(platRel.getToCiId(),plat2ExecOrderMap);
			}
		}

		int maxExecOrder = getMaxPlatExecOrder(plat2ExecOrderMap);
		for (long platId : plat2ExecOrderMap.keySet()) {
			CmsCI plat = plats.get(platId);
			if (plat.getCiState().equalsIgnoreCase("pending_deletion")
				|| disabledPlats.contains(plat.getCiId())) {
				plat2ExecOrderMap.put(platId, maxExecOrder+1);
			}
		}
		
		Map<Integer, List<CmsCI>> ExecOrder2PlatMap = new HashMap<Integer, List<CmsCI>>();

		for (long platId : plat2ExecOrderMap.keySet()) {
			if (!ExecOrder2PlatMap.containsKey(plat2ExecOrderMap.get(platId))) {
				ExecOrder2PlatMap.put(plat2ExecOrderMap.get(platId), new ArrayList<CmsCI>());
			}
			ExecOrder2PlatMap.get(plat2ExecOrderMap.get(platId)).add(plats.get(platId));
		}
		
		return ExecOrder2PlatMap;
	}
	
	private int getMaxPlatExecOrder(Map<Long, Integer> platMap) {
		int maxOrder = 0;
		for (Integer order : platMap.values()) {
			maxOrder = (order > maxOrder) ? order : maxOrder;
		}
		return maxOrder;
	}
	
	
	private void processPlatformsOrder(long startPlatId, Map<Long, Integer> platExecOrderMap) {

		List<CmsCIRelation> linksToRels = cmProcessor.getToCIRelationsNaked(startPlatId, "manifest.LinksTo", "manifest.Platform");
		int execOrder = platExecOrderMap.get(startPlatId) + 1;
		for (CmsCIRelation parentPlatLink : linksToRels) {
			if (!platExecOrderMap.containsKey(parentPlatLink.getFromCiId())){
				platExecOrderMap.put(parentPlatLink.getFromCiId(), execOrder);
			} else {
				if (platExecOrderMap.get(parentPlatLink.getFromCiId()) < execOrder) {
					platExecOrderMap.put(parentPlatLink.getFromCiId(),execOrder);
				}			
			}
			processPlatformsOrder(parentPlatLink.getFromCiId(), platExecOrderMap);
		}
	}
	
	
	private void commitManifestRelease(String manifestNsPath, String bomNsPath, String userId, String desc) {
		List<CmsRelease> manifestReleases = rfcProcessor.getReleaseBy3(manifestNsPath, null, "open");
		for (CmsRelease release : manifestReleases) {
			rfcProcessor.commitRelease(release.getReleaseId(), true, null,false,userId, desc);
		}
		// now we have a special case for the LinksTo relations
		// since nothing is really deleted but just marked as pending deletion until the bom is processed
		// but we need to delete LinksTo right now here because if nothing needs to be deployed or in case of circular
		// dependency the deployment will never happen
		List<CmsCIRelation> dLinkesToRels = cmProcessor.getCIRelationsNakedNoAttrsByState(manifestNsPath, "manifest.LinksTo", "pending_deletion", "manifest.Platform", "manifest.Platform");
		for (CmsCIRelation rel : dLinkesToRels) {
			cmProcessor.deleteRelation(rel.getCiRelationId(),true);
		}
		//same thing need to happened for monitors
		for (CmsCI monitor : cmProcessor.getCiByNsLikeByStateNaked(manifestNsPath, "manifest.Monitor", "pending_deletion")) {
			cmProcessor.deleteCI(monitor.getCiId(), true);
		}
		
		//if we have new manifest release - discard open bom release
		if (manifestReleases.size()>0) {
			List<CmsRelease> bomReleases = rfcProcessor.getReleaseBy3(bomNsPath, null, "open");
			for (CmsRelease bomRel : bomReleases) {
				bomRel.setReleaseState("canceled");
				rfcProcessor.updateRelease(bomRel);
			}
		}
	}

	private void check4openDeployment(String nsPath) {
		List<CmsDeployment>  dpmts = dpmtProcessor.findLatestDeployment(nsPath, null, false);
		if (dpmts.size()>0) {
			CmsDeployment dpmt = dpmts.get(0);
			if (dpmt.getDeploymentState().matches("active|failed|paused")) {
				String err = "There is an active deployment in this environment, you need to cancel or retry it";
				logger.error(err);
				throw new TransistorException(CmsError.TRANSISTOR_ACTIVE_DEPLOYMENT_EXISTS, err);
			}
		}
	}

}
