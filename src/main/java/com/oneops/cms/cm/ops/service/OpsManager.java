package com.oneops.cms.cm.ops.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.oneops.cms.cm.ops.domain.CmsActionOrder;
import com.oneops.cms.cm.ops.domain.CmsOpsAction;
import com.oneops.cms.cm.ops.domain.CmsOpsProcedure;
import com.oneops.cms.cm.ops.domain.OpsProcedureState;

/**
 * The Interface OpsManager.
 */
@Transactional
public interface OpsManager {
    CmsOpsProcedure createCmsOpsProcedure(CmsOpsProcedure proc);
    CmsOpsProcedure getCmsOpsProcedure(long procedureId, boolean definitionIncludes);
    List<CmsOpsProcedure> getCmsOpsProcedureForCi(long ciId, OpsProcedureState state, String procedureName, Integer limit);
    List<CmsOpsProcedure> getCmsOpsProcedureForCiByAction(long ciId, OpsProcedureState state, String procedureName, Integer limit);
    List<CmsOpsProcedure> getCmsOpsProcedureForNamespace(String nsPath, OpsProcedureState state, String procedureName);
    List<CmsOpsProcedure> getCmsOpsProceduresForCiFromTime(long ciId, String procedureName, Date timestamp);
   

	
    /**
     * return the list of procedure with their actions in org
     * @param nsPath the namespace for which the ops procedures need to be found.
     * @param state the state of the stored proc 
     * @param procedureName procedure name
     * @param limit number of procs to be returned
     * @return list of CmsOpsProcedure  
     * @see CmsOpsProcedure
     * @see OpsProcedureState
     */
     List<CmsOpsProcedure> getCmsOpsProcedureForNamespaceLike(String nsPath,
 			OpsProcedureState state, String procedureName, Integer limit,Boolean actions);
 	
    CmsOpsProcedure updateOpsProcedure(CmsOpsProcedure proc);
    CmsOpsProcedure retryOpsProcedure(long procId);
    CmsOpsProcedure updateProcedureState(long procId, OpsProcedureState state);
    CmsOpsAction updateOpsAction(CmsOpsAction action);

    //List<CmsActionOrder> getActionOrders(long procedureId, OpsProcedureState state, Integer execOrder);
    void completeActionOrder(CmsActionOrder ao);

	/**
	 * Submit procedure.
	 *
	 * @param proc the proc
	 * @return the cms ops procedure
	 */
	public CmsOpsProcedure submitProcedure(CmsOpsProcedure proc);
	
	/**
	 * Submit simple action.
	 *
	 * @param user the user
	 * @param anchorCiId the anchor ci id
	 * @param action the action
	 * @param argList the arg list
	 * @return the cms ops procedure
	 */
	public CmsOpsProcedure submitSimpleAction(String user, long anchorCiId, String action, Map<String,String> argList);
}
