
# 1. 本地library模块依赖
```groovy
implementation project(":mylibrary")
```

# 2. 本地二进制依赖
```groovy
implementation fileTree(dir: 'libs', include: ['*.jar'])
```

# 3. 远程二进制依赖
```groovy
implementation 'com.example.android:app-magic:12.3'
```

# 区别

## implementation

    与compile对应，会添加依赖到编译路径，并且会将依赖打包到输出（aar或apk），但是在编译时不会将依赖的实现暴露给其他module，也就是只有在运行时其他module才能访问这个依赖中的实现。使用这个配置，可以显著提升构建时间，因为它可以减少重新编译的module的数量。建议，尽量使用这个依赖配置。

## api

    与compile对应，功能完全一样，会添加依赖到编译路径，并且会将依赖打包到输出（aar或apk），与implementation不同，这个依赖可以传递，其他module无论在编译时和运行时都可以访问这个依赖的实现，也就是会泄漏一些不应该不使用的实现。举个例子，A依赖B，B依赖C，如果都是使用api配置的话，A可以直接使用C中的类（编译时和运行时），而如果是使用implementation配置的话，在编译时，A是无法访问C中的类的。

## compileOnly

    与provided对应，Gradle把依赖加到编译路径，编译时使用，不会打包到输出（aar或apk）。这可以减少输出的体积，在只在编译时需要，在运行时可选的情况，很有用。

## runtimeOnly

    与apk对应，gradle添加依赖只打包到APK，运行时使用，但不会添加到编译路径。这个没有使用过。


-----

[一文彻底搞清 Gradle 依赖](https://juejin.im/post/5c1700f5f265da614312f794)