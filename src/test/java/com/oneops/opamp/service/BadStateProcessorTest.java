package com.oneops.opamp.service;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.oneops.cms.cm.domain.CmsCI;
import com.oneops.cms.cm.domain.CmsCIRelation;
import com.oneops.cms.cm.ops.domain.CmsOpsProcedure;
import com.oneops.cms.cm.ops.domain.OpsProcedureDefinition;
import com.oneops.cms.cm.ops.service.OpsProcedureProcessor;
import com.oneops.cms.cm.service.CmsCmProcessor;
import com.oneops.cms.exceptions.OpsException;
import com.oneops.opamp.exceptions.OpampException;
import com.oneops.opamp.service.BadStateProcessor;
import com.oneops.opamp.service.EnvPropsProcessor;
import com.oneops.opamp.service.Notifications;
import com.oneops.opamp.util.EventUtil;
import com.oneops.ops.CiOpsProcessor;
import com.oneops.ops.events.CiChangeStateEvent;
import com.oneops.ops.events.OpsBaseEvent;

public class BadStateProcessorTest {

	@Test
	public void constructs(){
		BadStateProcessor badStateProcessor = new BadStateProcessor();
		badStateProcessor.setCmProcessor(new CmsCmProcessor());
		badStateProcessor.setCoProcessor(new CiOpsProcessor());
		badStateProcessor.setEnvProcessor(new EnvPropsProcessor());
		badStateProcessor.setNotifier(new Notifications());
		badStateProcessor.setOpsProcProcessor(new OpsProcedureProcessor());
    	
	}

    @Test(expectedExceptions = OpsException.class)
    public void simulateJmsError() throws OpampException {
        Long aCid = 678L;

        BadStateProcessor bad = new BadStateProcessor();
        //this mock will throw exception when asked to processProcedureRequest
        OpsProcedureProcessor opsProcedureProcessor = mock(OpsProcedureProcessor.class);
        EventUtil eventUtil = mock(EventUtil.class);
        bad.setEventUtil(eventUtil);
        when(opsProcedureProcessor.processProcedureRequest((CmsOpsProcedure) anyObject(), (OpsProcedureDefinition) anyObject()))
                .thenThrow(new OpsException(0, "mock-exception"));
        //wire the mocks here
        bad.setOpsProcProcessor(opsProcedureProcessor);
        OpsBaseEvent event = new OpsBaseEvent();
        event.setCiId(111L);
        CiOpsProcessor copMock = mock(CiOpsProcessor.class);
        when(copMock.getCIstate(aCid)).thenReturn("unhealthy");
        when(copMock.getCisOpenEvents(anyList())).thenReturn(null);

        bad.setCoProcessor(copMock);
        CiChangeStateEvent ciChangeStateEvent = new CiChangeStateEvent();
        bad.submitRepairProcedure(ciChangeStateEvent);

    }

	@Test
	/** where we send a good state request */
	public void processGoodStateTest(){
		Long aCid = 678L;

		BadStateProcessor bsp = new BadStateProcessor();
		
		bsp.setNotifier(mock(Notifications.class));
		
		EnvPropsProcessor envProcessorMock= mock(EnvPropsProcessor.class);
		CmsCI cmsCI = new CmsCI();
		
		when(envProcessorMock.isAutorepairEnbaled4bom(aCid)).thenReturn(cmsCI);
		bsp.setEnvProcessor(envProcessorMock);
		
		CmsCmProcessor cmProcessor = mock(CmsCmProcessor.class);
		List<CmsCIRelation> dependsList =new ArrayList<CmsCIRelation>();
		CmsCIRelation relation=new CmsCIRelation();
		relation.setFromCiId(aCid );
		dependsList.add(relation);
		
		when(cmProcessor.getToCIRelationsNakedNoAttrs(aCid, null, "DependsOn", null)).thenReturn(dependsList);
		bsp.setCmProcessor(cmProcessor);
		
		when(cmProcessor.getFromCIRelationsNakedNoAttrs(aCid, null, "DependsOn", null)).thenReturn(dependsList);

		CiOpsProcessor copMock=mock(CiOpsProcessor.class);
		when(copMock.getCIstate(aCid)).thenReturn("unhealthy");
		
		bsp.setCoProcessor(copMock);
		CiChangeStateEvent changeEvent = new CiChangeStateEvent();
		OpsBaseEvent event = new OpsBaseEvent();
		event.setCiId(aCid);
		EventUtil eventUtil = mock(EventUtil.class);
		bsp.setEventUtil(eventUtil);
		when(eventUtil.getOpsEvent(changeEvent)).thenReturn(event);
		bsp.processGoodState(changeEvent);
	}
	@Test
	public void processUnhealthyStateTest() throws OpampException{
		Long aCid = 321L;
		BadStateProcessor bsp = new BadStateProcessor();
		bsp.setNotifier(mock(Notifications.class));
			
		CiOpsProcessor copMock=mock(CiOpsProcessor.class);
		when(copMock.getCIstate(aCid)).thenReturn("unhealthy");
		bsp.setCoProcessor(copMock);
		
		EnvPropsProcessor envProcessorMock= mock(EnvPropsProcessor.class);
		when(envProcessorMock.isAutorepairEnbaled4bom(aCid)).thenReturn(null);
		bsp.setEnvProcessor(envProcessorMock);
		//autoRepair is not enabled, and so....here
		CiChangeStateEvent changeEvent = new CiChangeStateEvent();
	
		OpsBaseEvent event = new OpsBaseEvent();
		event.setCiId(aCid);
		EventUtil eventUtil = mock(EventUtil.class);
		bsp.setEventUtil(eventUtil);
		when(eventUtil.getOpsEvent(changeEvent)).thenReturn(event);

	
		
		bsp.processUnhealthyState(changeEvent); //notifier gets asked sendUnhealthyNotificationNoRepair		
		
	}
	@Test
	public void processUnhealthStateTestAR() throws OpampException{
		Long aCid = 1234L;
		BadStateProcessor bsp = new BadStateProcessor();
		List<CmsCIRelation> singleRel = new ArrayList<CmsCIRelation>(1);
		CmsCIRelation cmsCiRelation = new CmsCIRelation();
		
		singleRel.add(cmsCiRelation);

		CmsCmProcessor cmsCmProcessorMock = mock(CmsCmProcessor.class);
		when(cmsCmProcessorMock.getFromCIRelationsNakedNoAttrs(aCid,null, "DependsOn",null)).thenReturn(singleRel);
//			
		EnvPropsProcessor envProcessorYesMock= mock(EnvPropsProcessor.class);
		when(envProcessorYesMock.isAutorepairEnbaled4bom(aCid)).thenReturn(new CmsCI());

		CiOpsProcessor coProcessor = mock(CiOpsProcessor.class);
		when(coProcessor.getCIstate(anyLong())).thenReturn("actually-bad"); 
		//repairBad, but does not isDependsOnGood so just will tell the notifier
		bsp.setEnvProcessor(envProcessorYesMock);
		bsp.setCmProcessor(cmsCmProcessorMock);
		bsp.setCoProcessor(coProcessor);
		bsp.setNotifier(mock(Notifications.class));
		OpsBaseEvent event = new OpsBaseEvent();
		event.setCiId(aCid);
		CiChangeStateEvent changeEvent = new CiChangeStateEvent();
		
		EventUtil eventUtil = mock(EventUtil.class);
		bsp.setEventUtil(eventUtil);
		when(eventUtil.getOpsEvent(changeEvent)).thenReturn(event);
		bsp.processUnhealthyState(changeEvent);
	}
	

}
