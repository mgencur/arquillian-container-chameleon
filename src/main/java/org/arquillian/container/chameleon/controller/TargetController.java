package org.arquillian.container.chameleon.controller;


import static org.arquillian.container.chameleon.Utils.toMavenDependencies;
import static org.arquillian.container.chameleon.Utils.toURLs;

import java.io.File;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;

import org.arquillian.container.chameleon.ChameleonContainer;
import org.arquillian.container.chameleon.spi.model.ContainerAdapter;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependency;

public class TargetController {

    private ClassLoader classloader;
    @SuppressWarnings("rawtypes")
    private DeployableContainer delegate;

    private ContainerAdapter adapter;

    @SuppressWarnings("rawtypes")
    public TargetController(ContainerAdapter adapter, Injector injector, File resolverCacheFolder) throws Exception {
        // init
        this.classloader = resolveClasspathDependencies(adapter, resolverCacheFolder);
        final Class<?> delegateClass = classloader.loadClass(adapter.adapterClass());
        this.delegate = injector.inject((DeployableContainer) delegateClass.newInstance());
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    public Class<ContainerConfiguration> getConfigurationClass() {
        return delegate.getConfigurationClass();
    }

    public ClassLoader getClassLoader() {
        return this.classloader;
    }

    public ProtocolDescription getDefaultProtocol() {
        return this.adapter.overrideDefaultProtocol() ?
                new ProtocolDescription(adapter.getDefaultProtocol()):delegate.getDefaultProtocol();
    }

    public void setup(final ContainerConfiguration configuration) throws LifecycleException {
        lifecycle(new Callable<Void>() {
            @SuppressWarnings("unchecked")
            @Override
            public Void call() throws Exception {
                delegate.setup(configuration);
                return null;
            }
        });
    }

    public void start() throws LifecycleException {
        lifecycle(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                delegate.start();
                return null;
            }
        });
    }

    public void stop() throws LifecycleException {
        lifecycle(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                delegate.stop();
                return null;
            }
        });
    }

    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
        return deployment(new Callable<ProtocolMetaData>() {
            @SuppressWarnings("unchecked")
            @Override
            public ProtocolMetaData call() throws Exception {
                return delegate.deploy(archive);
            }
        });
    }

    public void undeploy(final Archive<?> archive) throws DeploymentException {
        deployment(new Callable<Void>() {
            @SuppressWarnings("unchecked")
            @Override
            public Void call() throws Exception {
                delegate.undeploy(archive);
                return null;
            }
        });
    }

    public void deploy(final Descriptor descriptor) throws DeploymentException {
        deployment(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                delegate.deploy(descriptor);
                return null;
            }
        });
    }

    public void undeploy(final Descriptor descriptor) throws DeploymentException {
        deployment(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                delegate.undeploy(descriptor);
                return null;
            }
        });
    }

    private <T> T deployment(Callable<T> callable) throws DeploymentException {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classloader);
            return callable.call();
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException("Could not proxy call", e);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    private <T> T lifecycle(Callable<T> callable) throws LifecycleException {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classloader);
            return callable.call();
        } catch (LifecycleException e) {
            throw e;
        } catch (Exception e) {
            throw new LifecycleException("Could not proxy call", e);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    private ClassLoader resolveClasspathDependencies(ContainerAdapter targetAdapter, File resolverCacheFolder) {
        String[] dependencies = targetAdapter.dependencies();

        try {
            MavenDependency[] mavenDependencies = toMavenDependencies(dependencies, targetAdapter.excludes());

            File[] archives = Resolver.resolve(resolverCacheFolder, mavenDependencies);
            return new URLClassLoader(toURLs(archives), ChameleonContainer.class.getClassLoader());

        } catch (Exception e) {
            throw new RuntimeException("Could not resolve target " + targetAdapter + " adapter dependencies", e);
        }
    }}
