package com.oneops.cms.ws.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.oneops.cms.ns.domain.CmsNamespace;
import com.oneops.cms.ns.service.CmsNsManager;

@Controller
public class NsRestController {

	private CmsNsManager nsManager;

	public void setNsManager(CmsNsManager nsManager) {
		this.nsManager = nsManager;
	}

	@RequestMapping(value="/ns/namespaces/{nsId}", method = RequestMethod.GET)
	@ResponseBody
	public CmsNamespace getNsById(@PathVariable long nsId){
		return nsManager.getNsById(nsId);
	}

	@RequestMapping(value="/ns/namespaces", method = RequestMethod.GET)
	@ResponseBody
	public List<CmsNamespace> getNs(
			@RequestParam(value="nsPath", required = true) String nsPath,
			@RequestParam(value="recursive", required = false)  Boolean recursive){

		if (recursive != null && recursive) {
			return nsManager.getNsLike(nsPath);
		} else {
			List<CmsNamespace> nsList = new ArrayList<CmsNamespace>();
			CmsNamespace ns = nsManager.getNs(nsPath);
			if (ns != null) {
				nsList.add(ns);
			}
			return nsList;
		}
	}
	
	@RequestMapping(method=RequestMethod.POST, value="/ns/namespaces")
	@ResponseBody
	public CmsNamespace createNs(@RequestBody CmsNamespace ns) {	
		return nsManager.createNs(ns);
	}

	@RequestMapping(method=RequestMethod.DELETE, value="/ns/namespaces/{nsId}")
	@ResponseBody
	public Map<String,String> deleteNs(@PathVariable long nsId) {	
		nsManager.deleteNsById(nsId);
		Map<String,String> result = new HashMap<String,String>();
		result.put("result","OK");
		return result;
	}

	@RequestMapping(value="/ns/namespaces/delete", method = RequestMethod.GET)
	@ResponseBody
	public Map<String,String> deleteNsPath(
			@RequestParam(value="nsPath", required = true) String nsPath){
		nsManager.deleteNs(nsPath);
		Map<String,String> result = new HashMap<String,String>();
		result.put("result","OK");
		return result;
	}
	
	
}
