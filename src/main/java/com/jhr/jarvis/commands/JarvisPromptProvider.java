/*
 * Copyright 2011-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jhr.jarvis.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.support.DefaultPromptProvider;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.shell.support.util.StringUtils;
import org.springframework.stereotype.Component;

import com.jhr.jarvis.service.ShipService;
import com.jhr.jarvis.service.StarSystemService;
import com.jhr.jarvis.service.StationService;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JarvisPromptProvider extends DefaultPromptProvider {

    @Autowired
    private StationService stationService;
    
    @Autowired
    private ShipService shipService;

    @Autowired
    private StarSystemService starSystemService;
    
	@Override
	public String getPrompt() {
	    
	    String prompt = "[";

        if (stationService.getUserLastStoredStation() != null) {
            prompt += stationService.getUserLastStoredStation().getName() + "@";
        }
	    
        if (!StringUtils.isEmpty(starSystemService.getUserLastStoredSystem())) {
            prompt += starSystemService.getUserLastStoredSystem();
        }
	    
		return OsUtils.LINE_SEPARATOR + prompt + "]--Jarvis>";
	}

	
	@Override
	public String getProviderName() {
		return "My prompt provider";
	}

}
