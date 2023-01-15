package com.lyr.hotfix

import android.annotation.TargetApi
import android.content.Context
import dalvik.system.DexClassLoader
import dalvik.system.PathClassLoader
import java.io.File
import java.lang.reflect.Array

object HotFix {

    fun patch(context: Context, patchDexFile: String?, patchClassName: String) {
        if (patchDexFile != null && File(patchDexFile).exists()) {
            try {
                if (hasDexClassLoader()) {
                    injectAboveEqualApiLevel14(context, patchDexFile, patchClassName)
                } else {
                    injectBelowApiLevel14(context, patchDexFile, patchClassName)
                }
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }

    private fun hasDexClassLoader(): Boolean {
        return try {
            Class.forName("dalvik.system.BaseDexClassLoader")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    @TargetApi(14)
    @Throws(
        ClassNotFoundException::class,
        NoSuchFieldException::class,
        IllegalAccessException::class
    )
    private fun injectBelowApiLevel14(context: Context, str: String, str2: String) {
        val obj = context.classLoader as PathClassLoader
        val dexClassLoader =
            DexClassLoader(str, context.getDir("dex", 0).absolutePath, str, context.classLoader)
        dexClassLoader.loadClass(str2)
        setField(
            obj, PathClassLoader::class.java, "mPaths",
            appendArray(
                getField(
                    obj,
                    PathClassLoader::class.java, "mPaths"
                ), getField(
                    dexClassLoader,
                    DexClassLoader::class.java,
                    "mRawDexPath"
                )
            )
        )
        setField(
            obj, PathClassLoader::class.java, "mFiles",
            combineArray(
                getField(
                    obj,
                    PathClassLoader::class.java, "mFiles"
                ), getField(
                    dexClassLoader,
                    DexClassLoader::class.java,
                    "mFiles"
                )
            )
        )
        setField(
            obj, PathClassLoader::class.java, "mZips",
            combineArray(
                getField(
                    obj,
                    PathClassLoader::class.java, "mZips"
                ), getField(
                    dexClassLoader,
                    DexClassLoader::class.java,
                    "mZips"
                )
            )
        )
        setField(
            obj, PathClassLoader::class.java, "mDexs",
            combineArray(
                getField(
                    obj,
                    PathClassLoader::class.java, "mDexs"
                ), getField(
                    dexClassLoader,
                    DexClassLoader::class.java,
                    "mDexs"
                )
            )
        )
        obj.loadClass(str2)
    }

    @Throws(
        ClassNotFoundException::class,
        NoSuchFieldException::class,
        IllegalAccessException::class
    )
    fun injectAboveEqualApiLevel14(context: Context, str: String, str2: String) {
        val pathClassLoader = context.classLoader as PathClassLoader
        val a = combineArray(
            getDexElements(getPathList(pathClassLoader)),
            getDexElements(
                getPathList(
                    DexClassLoader(
                        str,
                        context.getDir("dex", 0).absolutePath,
                        str,
                        context.classLoader
                    )
                )
            )
        )
        val a2 = getPathList(pathClassLoader)
        setField(a2, a2.javaClass, "dexElements", a)
        pathClassLoader.loadClass(str2)
    }

    @Throws(
        ClassNotFoundException::class,
        NoSuchFieldException::class,
        IllegalAccessException::class
    )
    private fun getPathList(obj: Any): Any {
        return getField(obj, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList")
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun getDexElements(obj: Any): Any {
        return getField(obj, obj.javaClass, "dexElements")
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun getField(obj: Any, cls: Class<*>, str: String): Any {
        val declaredField = cls.getDeclaredField(str)
        declaredField.isAccessible = true
        return declaredField[obj]
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun setField(obj: Any, cls: Class<*>, str: String, obj2: Any) {
        val declaredField = cls.getDeclaredField(str)
        declaredField.isAccessible = true
        declaredField[obj] = obj2
    }

    private fun combineArray(obj: Any, obj2: Any): Any {
        val componentType = obj2.javaClass.componentType
        val length = Array.getLength(obj2)
        val length2 = Array.getLength(obj) + length
        val newInstance = Array.newInstance(componentType, length2)
        for (i in 0 until length2) {
            if (i < length) {
                Array.set(newInstance, i, Array.get(obj2, i))
            } else {
                Array.set(newInstance, i, Array.get(obj, i - length))
            }
        }
        return newInstance
    }

    private fun appendArray(src: Any, dest: Any): Any {
        val componentType = src.javaClass.componentType
        val length = Array.getLength(src)
        val newInstance = Array.newInstance(componentType, length + 1)
        Array.set(newInstance, 0, dest)
        for (i in 1 until length + 1) {
            Array.set(newInstance, i, Array.get(src, i - 1))
        }
        return newInstance
    }
}