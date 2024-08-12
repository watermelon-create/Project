package com.mszlu.rpc.bean;

import com.mszlu.rpc.annontation.MsHttpClient;
import com.mszlu.rpc.annotation.EnableHttpClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Set;

public class MsBeanDefinitionRegistry implements ImportBeanDefinitionRegistrar,
        ResourceLoaderAware, EnvironmentAware {
    private Environment environment;
    private ResourceLoader resourceLoader;
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registerMsHttpClient(metadata,registry);
    }

    private void registerMsHttpClient(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        //将MsHttpClient所标识 的接口，生成代理类，并且注册到spring容器中
        Map<String, Object> annotationAttributes =
                metadata.getAnnotationAttributes(EnableHttpClient.class.getCanonicalName());
        Object basePackage=null;
        if(annotationAttributes!=null&&annotationAttributes.containsKey("basePackage"))
            basePackage= annotationAttributes.get("basePackage");
        if (basePackage != null){
            String base = basePackage.toString();
            //ClassPathScanningCandidateComponentProvider是Spring提供的工具，可以按自定义的类型，查找classpath下符合要求的class文件
            ClassPathScanningCandidateComponentProvider scanner = getScanner();
            scanner.setResourceLoader(resourceLoader);
            AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(MsHttpClient.class);
            scanner.addIncludeFilter(annotationTypeFilter);
            //上方定义了要找@MsHttpClient注解标识的类，这里进行对应包的扫描,扫描后就找到了所有被@MsHttpClient注解标识的类
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(base);
            //这里
            for (BeanDefinition candidateComponent:candidateComponents){
                if(candidateComponent instanceof AnnotatedBeanDefinition){
                    AnnotatedBeanDefinition annotatedBeanDefinition=(AnnotatedBeanDefinition)candidateComponent;
                    AnnotationMetadata beanannotationMetadata = annotatedBeanDefinition.getMetadata();
                    Assert.isTrue(beanannotationMetadata.isInterface(),"@MsHttpClient 必须定义在接口上");
                    Map<String, Object> clientAnnotationAttributes = beanannotationMetadata.
                            getAnnotationAttributes(MsHttpClient.class.getCanonicalName());
                    String beanName = getClientName(clientAnnotationAttributes);
                    //Bean的定义，通过建造者Builder模式来实现,需要一个参数，FactoryBean的实现类
                    //FactoryBean是一个工厂Bean，可以生成某一个类型Bean实例，它最大的一个作用是：可以让我们自定义Bean的创建过程。
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                            .genericBeanDefinition(MsHttpClientFactoryBean.class);
                    beanDefinitionBuilder.addPropertyValue("interfaceClass",
                            beanannotationMetadata.getClassName());
                    assert beanName != null;
                    registry.registerBeanDefinition(beanName,beanDefinitionBuilder.getBeanDefinition());
                }
            }
        }
    }
    private String getClientName(Map<String, Object> clientAnnotationAttributes) {
        if (clientAnnotationAttributes == null){
            throw new RuntimeException("value必须有值");
        }
        Object value = clientAnnotationAttributes.get("value");
        if (value != null && !value.toString().equals("")){
            return value.toString();
        }
        return null;
    }
    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }
    @Override
    public void setEnvironment(Environment environment) {
        this.environment=environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader=resourceLoader;
    }
}
