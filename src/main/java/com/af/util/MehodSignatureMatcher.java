package com.af.util;

import java.lang.reflect.Method;

public class MehodSignatureMatcher {
	/**
	 * 判断方法method的参数和params指定的参数是否兼容
	 * 
	 * @param method
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public static boolean isCompatible(final Method method, final Object[] args)
			throws Exception {
		final Class<?>[] targetTypes = method.getParameterTypes();
		if (args.length != targetTypes.length) {
			return false;
		}
		for (int i = 0; i < args.length; i++) {
			final Class<?> targetType = targetTypes[i];
			if (!isCompatible((Class<?>) args[i], targetType)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 两个类型是否兼容
	 * 
	 * @param srcType
	 * @param targetType
	 * @return
	 * @throws Exception
	 */
	private static boolean isCompatible(Class<?> srcType,
			final Class<?> targetType) throws Exception {
		if (targetType == srcType)
			return true;
		// int与Integer兼容
		if ((targetType == int.class && srcType == Integer.class)
				|| (targetType == Integer.class && srcType == int.class))
			return true;

		// 如果传入的是接口
		if (srcType.isInterface()) {
			if (hasInterface(srcType, targetType))
				return true;
			// 如果传入的是类
		} else {
			if (targetType.isInterface()) {
				while (srcType != null) {
					Class<?>[] facets = srcType.getInterfaces();
					for (Class<?> facet : facets) {
						// 看facet是否有
						if (hasInterface(facet, targetType))
							return true;
					}
					srcType = srcType.getSuperclass();
				}
				return false;
			} else {
				if (isSubclass(srcType, targetType))
					return true;
			}
		}
		return false;
	}

	/**
	 * srcType 实现了 targetType 接口中的一个
	 * 
	 * @param srcType
	 * @param targetType
	 * @return
	 */
	private static boolean hasInterface(Class<?> srcType, Class<?> targetType) {
		if (targetType.isInterface()) {
			if (targetType == srcType)
				return true;
			// 查看本身实现的接口
			Class<?>[] facets = srcType.getInterfaces();
			for (Class<?> facet : facets) {
				if (facet == targetType)
					return true;
				else
					return hasInterface(facet, targetType);
			}
			return false;
		} else {
			while (targetType != null) {
				// 查看本身实现的接口
				Class<?>[] facets = targetType.getInterfaces();
				for (Class<?> facet : facets) {
					if (facet == srcType)
						return true;
					else
						return hasInterface(srcType, facet);
				}
				targetType = targetType.getSuperclass();
			}
			return false;
		}
	}

	/**
	 * srcType 是 targetType 的子类
	 * 
	 * @param srcType
	 * @param targetType
	 * @return
	 */
	private static boolean isSubclass(Class<?> srcType, Class<?> targetType) {
		Class<?> superClass = srcType.getSuperclass();

		while (superClass != null) {
			if (superClass == targetType)
				return true;
			superClass = superClass.getSuperclass();
		}
		// 如果是Object.class，再判断一次
		if (superClass == targetType)
			return true;
		else
			return false;
	}

	/**
	 * 查找类clazz是否存在签名是args的method
	 * 
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public static Method getMatchingMethod(final Class<?> clazz,
			String methodName, final Object[] args) throws Exception {
		for (final Method method : clazz.getMethods()) {
			if (method.getName().equals(methodName)
					&& isCompatible(method, args)) {
				return method;
			}
		}
		return null;
	}

	public interface I1 {

	}

	public interface I2 {

	}

	public interface I3 extends I1, I2 {

	}

	public interface I4 extends I3 {

	}

	public class A implements I4 {

	}

	public class B extends A {

	}

	public class C extends B {

	}

	public static void test(A a, I1 i1) {

	}

	public static void main(String[] args) {
		try {
			// 完全匹配
			Method method = MehodSignatureMatcher.getMatchingMethod(
					MehodSignatureMatcher.class, "test", new Object[] {
							A.class, I1.class });
			System.out.println(method.getName());
			// 类传子类，接口传子接口
			method = MehodSignatureMatcher.getMatchingMethod(
					MehodSignatureMatcher.class, "test", new Object[] {
							C.class, I4.class });
			System.out.println(method.getName());
			// 类传子类，接口传子类
			method = MehodSignatureMatcher.getMatchingMethod(
					MehodSignatureMatcher.class, "test", new Object[] {
							C.class, C.class });
			System.out.println(method.getName());
			// 错误参数
			method = MehodSignatureMatcher.getMatchingMethod(
					MehodSignatureMatcher.class, "test", new Object[] {
							Object.class, C.class });
			System.out.println(method != null ? method.getName() : "null");
			// 错误参数
			method = MehodSignatureMatcher.getMatchingMethod(
					MehodSignatureMatcher.class, "test", new Object[] {
							C.class, Object.class });
			System.out.println(method != null ? method.getName() : "null");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
