package com.twb.websocket;

import io.reactivex.Completable;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Naik on 24.02.17.
 */
public interface ServiceRepository {

    @POST("hello-convert-and-send")
    Completable sendRestEcho(@Query("msg") String message);

}
