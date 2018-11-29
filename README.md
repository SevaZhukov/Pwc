# Paging with Caching Library

This library will help you to add caching in your paging list

![](https://habrastorage.org/webt/jp/fz/pt/jpfzptfobfumr_ri8yl8fznkssc.png)

### Add Pwc to your project

Add it in your root build.gradle at the end of repositories:

```Code
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

Add the dependency

```Code
dependencies {
  implementation 'com.github.MrSwimmer:Pwc:1.1'
}
```

### Main Components

#### PwcListing
Data class that is necessary for a UI to show a listing and interact w/ the rest of the system

#### NetworkState
Data class that is necessary for control network state

#### PwcPagingRequestHelper
A helper class for BoundaryCallback and DataSource to help with tracking network requests.
It is designed to support 3 types of requests: INITIAL, BEFORE and AFTER. And runs only 1 request 
for each of them via runIfNotRunning(RequestType, Request).
It tracks a Status and an error for each RequestType.

### Usage Sample

Check package 'sample' of this repository

### How it works?

Check this [post](https://habr.com/post/431212/) on Habr.com
