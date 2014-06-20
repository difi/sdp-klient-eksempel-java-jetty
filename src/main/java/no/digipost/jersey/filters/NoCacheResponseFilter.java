package no.digipost.jersey.filters;

import com.sun.jersey.spi.container.*;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

public class NoCacheResponseFilter implements ResourceFilter, ContainerResponseFilter {

	@Override
	public ContainerRequestFilter getRequestFilter() {
		return null;
	}

	@Override
	public ContainerResponseFilter getResponseFilter() {
		return this;
	}

	@Override
	public ContainerResponse filter(final ContainerRequest request, final ContainerResponse response) {
		Response res = Response.fromResponse(response.getResponse()).cacheControl(getNoCacheNoStoreCacheControl()).build();
		response.setResponse(res);
		return response;
	}

	public static CacheControl getNoCacheNoStoreCacheControl() {
		CacheControl cacheControl = new CacheControl();
		cacheControl.setNoCache(true);
		cacheControl.setNoStore(true);
		return cacheControl;
	}
}
