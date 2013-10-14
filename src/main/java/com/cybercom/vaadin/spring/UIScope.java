package com.cybercom.vaadin.spring;


import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.server.ClientConnector.DetachEvent;
import com.vaadin.server.ClientConnector.DetachListener;
import com.vaadin.ui.UI;

public class UIScope implements Scope, DetachListener, BeanFactoryPostProcessor {
	
	private final Map<UI, Map<String, Object>> objectMap = Collections.synchronizedMap(new HashMap<UI, Map<String, Object>>());
	private final Map<UI, Map<String, Runnable>> destructionCallbackMap = Collections.synchronizedMap(new HashMap<UI, Map<String, Runnable>>());

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		UI ui = UI.getCurrent();
		Map<String, Object> uiSpace = objectMap.get(ui);
		
		if (uiSpace == null) {
			ui.addDetachListener(this);
			uiSpace = Collections.synchronizedMap(new LinkedHashMap<String, Object>());
			objectMap.put(ui, uiSpace);
		}
		
		Object bean = uiSpace.get(name);
		if (bean == null) {
			bean = objectFactory.getObject();
			uiSpace.put(name, bean);
		}
		return bean;
	}

	@Override
	public Object remove(String name) {
		UI ui = UI.getCurrent();
		Map<String, Runnable> destructionSpace = destructionCallbackMap.get(ui);
		if (destructionSpace != null) {
			destructionSpace.remove(name);
		}
		Map<String, Object> uiSpace = objectMap.get(ui);
		if (uiSpace == null) {
			return null;
		}
		return uiSpace.remove(name);
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		UI ui = UI.getCurrent();
		Map<String, Runnable> destructionSpace = destructionCallbackMap.get(ui);
		if (destructionSpace == null) {
			destructionSpace = Collections.synchronizedMap(new HashMap<String, Runnable>());
			destructionCallbackMap.put(ui, destructionSpace);
		}
		destructionSpace.put(name, callback);
	}

	@Override
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		UI ui = UI.getCurrent();
		return ui == null ? null : ui.getId();  
	}

	@Override
	public void detach(DetachEvent event) {
		UI ui = (UI)event.getSource();
		
		Map<String, Runnable> destructionSpace = destructionCallbackMap.remove(ui);
		if (destructionSpace != null) {
			for (Runnable runnable : destructionSpace.values()) {
				runnable.run();
			}
		}
		
		objectMap.remove(ui);
	}

	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		beanFactory.registerScope("ui", this);
	}

}
