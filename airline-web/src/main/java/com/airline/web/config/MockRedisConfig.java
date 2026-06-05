package com.airline.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Profile("local")
public class MockRedisConfig {

    public static class LocalMockRedisTemplate extends RedisTemplate<String, String> {
        private final Map<String, String> store;
        private final ValueOperations<String, String> valueOps;
        private final ListOperations<String, String> listOps;

        @SuppressWarnings("unchecked")
        public LocalMockRedisTemplate(Map<String, String> store, Map<String, List<String>> lists) {
            this.store = store;

            // Create Proxy for ValueOperations
            this.valueOps = (ValueOperations<String, String>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class[]{ValueOperations.class},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            String methodName = method.getName();
                            if (methodName.equals("hashCode")) {
                                return System.identityHashCode(proxy);
                            } else if (methodName.equals("equals")) {
                                return proxy == args[0];
                            } else if (methodName.equals("toString")) {
                                return "MockValueOperations";
                            } else if (methodName.equals("set")) {
                                store.put((String) args[0], (String) args[1]);
                                return null;
                            } else if (methodName.equals("get")) {
                                return store.get((String) args[0]);
                            } else if (methodName.equals("setIfAbsent")) {
                                String key = (String) args[0];
                                String val = (String) args[1];
                                String existing = store.putIfAbsent(key, val);
                                return existing == null;
                            }
                            return null;
                        }
                    }
            );

            // Create Proxy for ListOperations
            this.listOps = (ListOperations<String, String>) Proxy.newProxyInstance(
                    ListOperations.class.getClassLoader(),
                    new Class[]{ListOperations.class},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            String methodName = method.getName();
                            if (methodName.equals("hashCode")) {
                                return System.identityHashCode(proxy);
                            } else if (methodName.equals("equals")) {
                                return proxy == args[0];
                            } else if (methodName.equals("toString")) {
                                return "MockListOperations";
                            } else if (methodName.equals("rightPush")) {
                                String key = (String) args[0];
                                String val = (String) args[1];
                                lists.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
                                return 1L;
                            } else if (methodName.equals("leftPop")) {
                                String key = (String) args[0];
                                List<String> list = lists.get(key);
                                if (list == null || list.isEmpty()) {
                                    return null;
                                }
                                return list.remove(0);
                            }
                            return null;
                        }
                    }
            );
        }

        @Override
        public ValueOperations<String, String> opsForValue() {
            return this.valueOps;
        }

        @Override
        public ListOperations<String, String> opsForList() {
            return this.listOps;
        }

        @Override
        public Boolean hasKey(String key) {
            return this.store.containsKey(key);
        }

        @Override
        public Boolean delete(String key) {
            return this.store.remove(key) != null;
        }

        @Override
        public void afterPropertiesSet() {
            // Override to prevent trying to connect to Redis
        }
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return (RedisConnectionFactory) Proxy.newProxyInstance(
                RedisConnectionFactory.class.getClassLoader(),
                new Class[]{RedisConnectionFactory.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        if (name.equals("hashCode")) {
                            return System.identityHashCode(proxy);
                        } else if (name.equals("equals")) {
                            return proxy == args[0];
                        } else if (name.equals("toString")) {
                            return "MockRedisConnectionFactory";
                        }
                        return null;
                    }
                }
        );
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        final Map<String, String> store = new ConcurrentHashMap<>();
        final Map<String, List<String>> lists = new ConcurrentHashMap<>();
        return new LocalMockRedisTemplate(store, lists);
    }
}
