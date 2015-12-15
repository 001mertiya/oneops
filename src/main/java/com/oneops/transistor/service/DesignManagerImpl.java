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
package com.oneops.transistor.service;

import com.oneops.cms.cm.domain.CmsCI;
import com.oneops.cms.dj.domain.CmsRfcCI;
import com.oneops.transistor.domain.CatalogExport;

public class DesignManagerImpl implements DesignManager {

	private DesignRfcProcessor designRfcProcessor;
	private CatalogProcessor catalogProcessor;

	public void setDesignRfcProcessor(DesignRfcProcessor designRfcProcessor) {
		this.designRfcProcessor = designRfcProcessor;
	}

	public void setCatalogProcessor(CatalogProcessor catalogProcessor) {
		this.catalogProcessor = catalogProcessor;
	}

	@Override
	public long generatePlatform(CmsRfcCI platRfc, long assemblyId,
			String userId, String scope) {
		return designRfcProcessor.generatePlatFromTmpl(platRfc, assemblyId, userId, scope);
	}

	@Override
	public long clonePlatform(CmsRfcCI platRfc, Long targetAssemblyId,
			long sourcePlatId, String userId, String scope) {
		return designRfcProcessor.clonePlatform(platRfc, targetAssemblyId, sourcePlatId, userId, scope);
	}

	@Override
	public long cloneAssembly(CmsCI assemblyCI, 
			long sourceAssemblyId, String userId, String scope) {
		return designRfcProcessor.cloneAssembly(assemblyCI, sourceAssemblyId, userId, scope);
	}

	@Override
	public long saveAssemblyAsCatalog(CmsCI catalogCI, 
			long sourceAssemblyId, String userId, String scope) {
		return catalogProcessor.saveAssemblyAsCatalog(catalogCI, sourceAssemblyId, userId, scope);
	}

	@Override
	public CatalogExport exportCatalog(long catalogCIid, String scope) {
		return catalogProcessor.exportCatalog(catalogCIid, scope);
	}

	@Override
	public long importCatalog(CatalogExport catExp, String userId,
			String scope) {
		return catalogProcessor.importCatalog(catExp, userId, scope);
	}

	@Override
	public long deletePlatform(long platformId, String userId, String scope) {
		return designRfcProcessor.deletePlatform(platformId, userId, scope);
	}


}
