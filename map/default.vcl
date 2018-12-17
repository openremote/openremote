vcl 4.0;

sub vcl_recv {
  unset req.http.Cookie;
}


sub vcl_backend_response {
    set beresp.ttl = 1000000d;
    unset beresp.http.cookie;
    unset beresp.http.Set-Cookie;
    unset beresp.http.Cache-Control;
}

backend default {
    .host = "127.0.0.1";
    .port = "8080";
}