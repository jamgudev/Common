package com.jamgu.common.util.reflect

import android.text.TextUtils
import android.util.Log
import com.jamgu.common.util.log.JLog.e
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashSet

object ReflectHelper {
    private const val TAG = "ReflectHelper"
    @JvmStatic
    fun getField(clazz: Class<*>, fieldName: String?, instance: Any?): Any? {
        val field: Field
        return try {
            field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field[instance]
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun getField(className: String?, fieldName: String?, instance: Any?): Any? {
        return try {
            val clazz = Class.forName(className)
            getField(clazz, fieldName, instance)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun setField(clazz: Class<*>, fieldName: String?, value: Any?, instance: Any?) {
        val field: Field
        try {
            field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field[instance] = value
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun setField(className: String?, fieldName: String?, value: Any?, instance: Any?) {
        try {
            val clazz = Class.forName(className)
            setField(clazz, fieldName, value, instance)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取某对象的指定名字的基类
     *
     * @param object    目标对象
     * @param className 目标类名
     * @return class
     */
    @JvmStatic
    fun getSuperclass(`object`: Any?, className: String): Class<*>? {
        if (`object` != null && !TextUtils.isEmpty(className)) {
            var clazz: Class<*>? = `object`.javaClass
            do {
                clazz = if (clazz!!.simpleName == className) {
                    return clazz
                } else {
                    clazz.superclass
                }
            } while (clazz != null)
        } else {
            e(TAG, "getSuperclass err")
        }
        return null
    }

    @JvmStatic
    fun invokeMethod(
        clazz: Class<*>,
        methodName: String?,
        instance: Any?,
        paramTypes: Array<Class<*>?>,
        params: Array<Any?>
    ): Any? {
        val method: Method
        return try {
            method = clazz.getDeclaredMethod(methodName, *paramTypes)
            method.isAccessible = true
            method.invoke(instance, *params)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun invokeMethod(
        className: String?,
        methodName: String?,
        instance: Any?,
        paramTypes: Array<Class<*>?>,
        params: Array<Any?>
    ): Any? {
        return try {
            val clazz = Class.forName(className)
            invokeMethod(clazz, methodName, instance, paramTypes, params)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun getInstanceCount(find: Class<*>?): Long {
        val start = System.currentTimeMillis()
        var result: Long = 0
        result = try {
            val clazz = Class.forName("dalvik.system.VMDebug")
            val method = clazz.getMethod("countInstancesOfClass", Class::class.java, Boolean::class.javaPrimitiveType)
            method.invoke(null, find, false) as Long
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
        Log.e("fuck", "use time===" + (System.currentTimeMillis() - start))
        return result
    }

    @JvmStatic
    fun getAllInterfaces(cls: Class<*>?): List<Class<*>>? {
        if (cls == null) {
            return null
        }
        val interfacesFound = LinkedHashSet<Class<*>>()
        getAllInterfaces(cls, interfacesFound)
        return ArrayList(interfacesFound)
    }

    @JvmStatic
    private fun getAllInterfaces(cls: Class<*>, interfacesFound: HashSet<Class<*>>) {
        var cls: Class<*>? = cls
        while (cls != null) {
            val interfaces = cls.interfaces
            for (i in interfaces) {
                if (interfacesFound.add(i)) {
                    getAllInterfaces(i, interfacesFound)
                }
            }
            cls = cls.superclass
        }
    }
}