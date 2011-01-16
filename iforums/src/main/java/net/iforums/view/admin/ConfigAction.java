/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * This file creation date: 15/08/2003 / 20:56:33
 * The JForum Project
 * http://www.jforum.net
 */
package net.iforums.view.admin;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.iforums.beans.Category;
import net.iforums.beans.Forum;
import net.iforums.context.RequestContext;
import net.iforums.context.ResponseContext;
import net.iforums.repository.ForumRepository;
import net.iforums.repository.TopicRepository;
import net.iforums.utils.I18n;
import net.iforums.utils.preferences.ConfigKeys;
import net.iforums.utils.preferences.SystemGlobals;
import net.iforums.utils.preferences.TemplateKeys;
import freemarker.template.SimpleHash;

/**
 * @author Rafael Steil
 * @version $Id: ConfigAction.java,v 1.21 2007/07/28 14:17:10 rafaelsteil Exp $
 */
public class ConfigAction extends AdminCommand 
{
	public ConfigAction() {}
	
	public ConfigAction(RequestContext request,
			ResponseContext response, 
			SimpleHash context)
	{
		this.request = request;
		this.response = response;
		this.context = context;
	}
	
	public void list() {
		Properties p = new Properties();
		Iterator iter = SystemGlobals.fetchConfigKeyIterator();

		while (iter.hasNext()) {
			String key = (String) iter.next();
			String value = SystemGlobals.getValue(key);
			p.put(key, value);
		}

		Properties locales = new Properties();
		
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(SystemGlobals.getValue(ConfigKeys.CONFIG_DIR)
				+ "/languages/locales.properties");
			locales.load(fis);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (fis != null) {
				try { fis.close(); } catch (Exception e) {}
			}
		}
		
		List localesList = new ArrayList();

		for (Enumeration e = locales.keys(); e.hasMoreElements();) {
			localesList.add(e.nextElement());
		}

		this.context.put("config", p);
		this.context.put("locales", localesList);
		this.setTemplateName(TemplateKeys.CONFIG_LIST);
	}

	public void editSave()
	{
		this.updateData(this.getConfig());
		this.list();
	}
	
	Properties getConfig()
	{
		Properties p = new Properties();

		Enumeration e = this.request.getParameterNames();
		while (e.hasMoreElements()) {
			String name = (String) e.nextElement();

			if (name.startsWith("p_")) {
				p.setProperty(name.substring(name.indexOf('_') + 1), this.request.getParameter(name));
			}
		}
		
		return p;
	}
	
	void updateData(Properties p)
	{
		int oldTopicsPerPage = SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE);

		for (Iterator iter = p.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry entry = (Map.Entry)iter.next();
			
			SystemGlobals.setValue((String)entry.getKey(), (String)entry.getValue());
		}
		
		SystemGlobals.saveInstallation();
		I18n.changeBoardDefault(SystemGlobals.getValue(ConfigKeys.I18N_DEFAULT));
		
		// If topicsPerPage has changed, force a reload in all forums
		if (oldTopicsPerPage != SystemGlobals.getIntValue(ConfigKeys.TOPICS_PER_PAGE)) {
			List categories = ForumRepository.getAllCategories();
			
			for (Iterator iter = categories.iterator(); iter.hasNext(); ) {
				Category c = (Category)iter.next();
				
				for (Iterator iter2 = c.getForums().iterator(); iter2.hasNext(); ) {
					Forum f = (Forum)iter2.next();
					TopicRepository.clearCache(f.getId());
				}
			}
		}
	}
}