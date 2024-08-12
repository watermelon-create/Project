package com.mszlu.rpc.spring;

import com.mszlu.rpc.annotation.EnableRpc;
import com.mszlu.rpc.config.MsRpcConfig;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.annotation.MsReference;
import com.mszlu.rpc.annotation.MsService;
import com.mszlu.rpc.netty.client.NettyClient;
import com.mszlu.rpc.proxy.MsRpcClientProxy;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import com.mszlu.rpc.server.MsServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
@Slf4j
@Component
public class MsRpcSpringBeanPostProcessor implements BeanPostProcessor,BeanFactoryPostProcessor{
//    , BeanFactoryPostProcessor
    private MsServiceProvider msServiceProvider;
    private MsRpcConfig msRpcConfig;
    private NettyClient nettyClient;
    private NacosTemplate nacosTemplate;
    public MsRpcSpringBeanPostProcessor() {
        //防止线程问题、便于其他类使用
        this.msServiceProvider = SingletonFactory.getInstance(MsServiceProvider.class);
        nettyClient= SingletonFactory.getInstance(NettyClient.class);
        nacosTemplate=SingletonFactory.getInstance(NacosTemplate.class);
    }

    //bean初始化方法调用后被调用
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //在这里判断bean上有没有加MsService注解
        //如果有服务提供者，将其发布为服务
        System.out.println(bean);
        if(bean.getClass().isAnnotationPresent(MsService.class)){
            MsService annotation = bean.getClass().getAnnotation(MsService.class);
            //加了MsService的Bean 就被找到了，就把其中的方法都发布为服务
            msServiceProvider.publishService(annotation,bean);

        }

        //服务消费者代理实现类调用--在声明的字段上查找MsReference注解
        Class<?> targetClass=bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for(Field declaredField:declaredFields){
            MsReference annotation = declaredField.getAnnotation(MsReference.class);
            if (annotation != null) {
                //加了MsReference的字段 就被找到了，生成代理类，当接口方法调用的时候，实际上就是访问的代理类
                //代理实现类，调用方法的时候 会触发invoke方法，在其中实现网络调用
                MsRpcClientProxy msRpcClientProxy = new MsRpcClientProxy(annotation,nettyClient);
                Object proxy=msRpcClientProxy.getProxy(declaredField.getType());
                //当isAccessible()的结果是false时不允许通过反射访问该字段
                declaredField.setAccessible(true);
                try {
                    /**
                     * 将指定的对象 proxy 设置为 bean 对象中 declaredField 字段的值。
                     */
                    declaredField.set(bean,proxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        EnableRpc enableRpc = bean.getClass().getAnnotation(EnableRpc.class);
        if (enableRpc!=null) {
            if (msRpcConfig==null) {
                log.info("EnableRpc 会先于所有的Bean实例化之前 执行");
                msRpcConfig=new MsRpcConfig();
                msRpcConfig.setNacosGroup(enableRpc.nacosGroup());
                msRpcConfig.setNacosPort(enableRpc.nacosPort());
                msRpcConfig.setNacosHost(enableRpc.nacosHost());
                msRpcConfig.setProviderPort(enableRpc.serverPort());
                msRpcConfig.setLoadBalance(enableRpc.loadBalance());
                nettyClient.setMsRpcConfig(msRpcConfig);
                msServiceProvider.setMsRpcConfig(msRpcConfig);
                nacosTemplate.init(msRpcConfig.getNacosHost(),msRpcConfig.getNacosPort());
            }

        }
        return bean;
    }

    /**
     *扫描整个应用的注解,拿到对应的配置。其中enableRpc注解会先于所有的bean实例化之前进行加载
     * @param beanFactory
     * @throws BeansException
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner--定义扫描器
                Class<?> scannerClass = ClassUtils.forName ( "org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
                        MsRpcSpringBeanPostProcessor.class.getClassLoader () );
                Object scanner = scannerClass.getConstructor ( new Class<?>[]{BeanDefinitionRegistry.class, boolean.class} )
                        .newInstance ( new Object[]{(BeanDefinitionRegistry) beanFactory, true} );
                // add filter
                Class<?> filterClass = ClassUtils.forName ( "org.springframework.core.type.filter.AnnotationTypeFilter",
                        MsRpcSpringBeanPostProcessor.class.getClassLoader () );
                Object filter = filterClass.getConstructor ( Class.class ).newInstance ( EnableRpc.class );
                Method addIncludeFilter = scannerClass.getMethod ( "addIncludeFilter",
                        ClassUtils.forName ( "org.springframework.core.type.filter.TypeFilter", MsRpcSpringBeanPostProcessor.class.getClassLoader () ) );
                addIncludeFilter.invoke ( scanner, filter );
                // scan packages
                Method scan = scannerClass.getMethod ( "scan", new Class<?>[]{String[].class} );
                scan.invoke ( scanner, new Object[]{"com.mszlu.rpc.annontation"} );
            } catch (Throwable e) {
                // spring 2.0

            }
        }
    }
}
