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
import org.springframework.shell.plugin.support.DefaultBannerProvider;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;

import com.jhr.jarvis.service.ShipService;
import com.jhr.jarvis.service.StarSystemService;
import com.jhr.jarvis.service.StationService;
import com.jhr.jarvis.service.TradeService;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JarvisBannerProvider extends DefaultBannerProvider  {
    
    @Autowired
    private TradeService tradeService;

    @Autowired
    private StationService stationService;
    
    @Autowired
    private StarSystemService starSystemService;


	public String getBanner() {
		StringBuffer buf = new StringBuffer();
		buf.append(OsUtils.LINE_SEPARATOR);
		buf.append("=======================================" + OsUtils.LINE_SEPARATOR);
		buf.append("*                                     *"+ OsUtils.LINE_SEPARATOR);
		buf.append("*            --Jarvis--               *" +OsUtils.LINE_SEPARATOR);
		buf.append("*                                     *"+ OsUtils.LINE_SEPARATOR);
		buf.append("=======================================" + OsUtils.LINE_SEPARATOR);
		buf.append("Version:          " + getVersion() + OsUtils.LINE_SEPARATOR);
		buf.append("Systems:          " + starSystemService.systemCountOrientDb() + OsUtils.LINE_SEPARATOR);
        buf.append("Frameshift Edges: " + starSystemService.shiftCountOrientDb() + OsUtils.LINE_SEPARATOR);
		buf.append("Stations:         " + stationService.stationCountOrientDb() + OsUtils.LINE_SEPARATOR);
        buf.append("Exchanges:        " + tradeService.exchangeCountOrientDb() + OsUtils.LINE_SEPARATOR);
        
		
		return buf.toString();
	}

	public String getVersion() {
		return "3";
	}

	public String getWelcomeMessage() {
		return "Welcome to Jarvis CLI";
	}
	
	@Override
	public String getProviderName() {
		return "Jarvis";
	}
}