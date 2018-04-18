# jaxws.cache
Websphere Application Server servlet cache HTTP 1.1 Transfer-Encoding: chunked support

WAS Servlet Cache documentation:
> https://www.ibm.com/support/knowledgecenter/en/SSAW57_9.0.0/com.ibm.websphere.nd.multiplatform.doc/ae/tdyn_dynamiccacheconfig.html

This repo enables support for HTTP 1.1 Header "Transfer-Encoding: chunked", add:
```
<idgenerator>com.ibm.expiremental.jaxws.cache.JaxwsIdGeneratorSHA256S</idgenerator>
```

Example usage:
```
<?xml version="1.0" ?>
<!DOCTYPE cache SYSTEM "cachespec.dtd">

<cache>
	<cache-entry>
		<class>webservice</class>
		<name>/app/jaxws-service</name>
		<sharing-policy>not-shared</sharing-policy>

		<cache-id>
			<idgenerator>com.ibm.expiremental.jaxws.cache.JaxwsIdGeneratorSHA256S</idgenerator>
			<timeout>60</timeout>
			<priority>1</priority>
		</cache-id> 
 
 </cache-entry>
</cache>
```

This implementation supports _SoapAction_ filtering:<br>
Place __cache-enable.properties__ in _$WAS_PROFILE/properties_ folder

Example:
```
SoapAction1
SoapAction2=true
SoapAction3=false
.*Action5=wildcard
.*Action6=wildcardOff
```

This will act as:
```
SoapAction1 - enabled
SoapAction2 - enabled
SoapAction3 - disabled
SoapAction4 or xxxAction4 - enabled (as Regexp)
SoapAction5 or xxxAction5 - disabled (as Regexp)
```

Note: _disabled_ > _enabled_ > _wildcardOff_ > _wildcard_
