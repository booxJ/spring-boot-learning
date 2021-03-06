package com.springboot.booxj.invokeBeanFactoryPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.Configuration;

/**
 * spring容器初始化时，从资源中读取到bean的相关定义后，保存在beanFactory的成员变量中，在实例化bean的操作就是依据这些bean的定义来做的，
 * 而在实例化之前，spring允许我们通过自定义扩展来改变bean的定义，定义一旦变了，后面的实例也就变了，BeanFactoryPostProcessor就是用来改变bean定义的
 * <p>
 * AbstractApplicationContext.refresh()->invokeBeanFactoryPostProcessors(beanFactory)->
 * PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors()
 */
@Configuration
public class CustomizeBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        AbstractBeanDefinition abstractBeanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition("helloService");
        MutablePropertyValues pv = abstractBeanDefinition.getPropertyValues();
        pv.addPropertyValue("desc", "Desc is changed from bean factory post processor");
        abstractBeanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
    }
}
