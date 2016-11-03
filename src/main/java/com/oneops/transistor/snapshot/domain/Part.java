package com.oneops.transistor.snapshot.domain;

import java.util.*;

/*******************************************************************************
 *
 *   Copyright 2016 Walmart, Inc.
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
public class Part {
    private String ns;
    private String className;
    private Map<String, List<ExportCi>> cis= new TreeMap<>();
    private Map<Long, ExportCi> ciMap= new HashMap<>();


    public Part() {
    }

    public Part(String namespace, String clazzName) {
        this.ns = namespace;
        this.className = clazzName;
    }

    public String getNs() {
        return ns;
    }

    public void setNs(String ns) {
        this.ns = ns;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Map<String, List<ExportCi>> getCis() {
        return cis;
    }

    public void setCis(Map<String, List<ExportCi>> cis) {
        this.cis = cis;
    }

    public void addExportCi(String actualNs, ExportCi exportCi){
        ciMap.put(exportCi.getId(), exportCi);
        List<ExportCi> list = cis.get(actualNs);
        if (list==null) {
            list = new ArrayList<>();
            cis.put(actualNs, list);
        }
        list.add(exportCi);
    }

    public void addExportRelations(long from, ExportRelation exportRelation) {
        ExportCi exportCi = ciMap.get(from);
        if (exportCi!=null) {
            exportCi.addRelation(exportRelation);
        }
    }
}
