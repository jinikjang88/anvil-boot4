package com.devsmith.anvil.ch03.service;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 살아 있는 Spring 컨테이너의 의존성 그래프를 JSON 으로 노출한다.
 *
 * <p>D3 시각화의 데이터 소스. 학습 포인트 — Spring 이 실제로 어떻게 빈을 엮었는지를
 * 리플렉션으로 들여다본다 (운영용은 아님, 학습용).</p>
 *
 * <p>그래프 산정 규칙:
 * <ol>
 *   <li>{@code com.devsmith.anvil.ch03} 패키지 빈만 노드로 포함</li>
 *   <li>각 빈의 <b>파라미터가 가장 많은 생성자</b> 를 "주" 생성자로 간주</li>
 *   <li>생성자 인자가 우리 패키지의 클래스/인터페이스면 그쪽으로 엣지</li>
 *   <li>{@code Map<String, X>} / {@code Collection<X>} 인자는 <b>X 타입의 모든 빈</b> 으로 펼침
 *       — Spring 의 컬렉션 자동 수집을 시각적으로 드러내기 위함</li>
 *   <li>외부 의존({@link Clock}) 은 "external" 카테고리로 별도 표시</li>
 * </ol>
 * </p>
 */
@Service
public class BeanGraphService {

    private static final String SCOPE_PACKAGE = "com.devsmith.anvil.ch03";

    private final ApplicationContext context;

    public BeanGraphService(ApplicationContext context) {
        this.context = context;
    }

    public Graph snapshot() {
        Map<String, Node> nodes = new LinkedHashMap<>();
        List<Edge> edges = new ArrayList<>();

        for (String beanName : context.getBeanDefinitionNames()) {
            Class<?> beanClass = userClassOf(beanName);
            if (beanClass == null || !inScope(beanClass)) {
                continue;
            }

            String label = beanClass.getSimpleName();
            nodes.putIfAbsent(label, new Node(label, kindOf(beanClass)));

            for (Dep dep : collectDependencies(beanClass)) {
                String depLabel = dep.type().getSimpleName();
                if (depLabel.equals(label)) {
                    continue;
                }
                edges.add(new Edge(label, depLabel));
                nodes.putIfAbsent(depLabel, new Node(depLabel, dep.nodeKind()));
            }
        }

        return new Graph(List.copyOf(nodes.values()), edges);
    }

    // ─── 의존성 추출 ──────────────────────────────────────────────────

    private List<Dep> collectDependencies(Class<?> beanClass) {
        Constructor<?> ctor = primaryConstructor(beanClass);
        if (ctor == null || ctor.getParameterCount() == 0) {
            return List.of();
        }

        List<Dep> deps = new ArrayList<>();
        Parameter[] parameters = ctor.getParameters();
        for (Parameter param : parameters) {
            Class<?> paramType = param.getType();

            if (Map.class.isAssignableFrom(paramType) || Collection.class.isAssignableFrom(paramType)) {
                // Map<String, Channel> / List<Channel> 의 value/element 타입을 꺼내 펼친다
                Class<?> innerType = innerCollectionType(param.getParameterizedType(), paramType);
                if (innerType != null) {
                    for (String name : context.getBeanNamesForType(innerType)) {
                        Class<?> implClass = userClassOf(name);
                        if (implClass != null && inScope(implClass)) {
                            deps.add(new Dep(implClass, kindOf(implClass)));
                        }
                    }
                }
            } else if (inScope(paramType)) {
                deps.add(new Dep(paramType, kindOf(paramType)));
            } else if (paramType == Clock.class) {
                deps.add(new Dep(paramType, "external"));
            }
            // 그 외(예: ApplicationContext, Environment 등) 는 의도적으로 무시
        }
        return deps;
    }

    private static Constructor<?> primaryConstructor(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElse(null);
    }

    private static Class<?> innerCollectionType(Type parameterizedType, Class<?> rawType) {
        if (!(parameterizedType instanceof ParameterizedType pt)) {
            return null;
        }
        Type[] args = pt.getActualTypeArguments();
        if (args.length == 0) {
            return null;
        }
        // Map: value (index 1) / Collection: element (index 0)
        Type target = Map.class.isAssignableFrom(rawType) ? args[args.length - 1] : args[0];
        return target instanceof Class<?> c ? c : null;
    }

    // ─── 분류 헬퍼 ────────────────────────────────────────────────────

    private Class<?> userClassOf(String beanName) {
        try {
            Object bean = context.getBean(beanName);
            return ClassUtils.getUserClass(bean);   // CGLIB 프록시 제거
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean inScope(Class<?> clazz) {
        return clazz.getPackageName().startsWith(SCOPE_PACKAGE);
    }

    private static String kindOf(Class<?> clazz) {
        if (clazz.isAnnotationPresent(RestController.class)) return "controller";
        if (clazz.isAnnotationPresent(Service.class))        return "service";
        if (clazz.isAnnotationPresent(Configuration.class))  return "config";
        if (clazz.isAnnotationPresent(Component.class))      return "component";
        return "bean";
    }

    // ─── 그래프 DTO ──────────────────────────────────────────────────

    public record Graph(List<Node> nodes, List<Edge> edges) { }
    public record Node(String id, String kind) { }
    public record Edge(String from, String to) { }

    private record Dep(Class<?> type, String nodeKind) { }
}
