vcl 4.0;

sub vcl_recv {
  unset req.http.Cookie;
}

backend default {
    .host = "127.0.0.1";
    .port = "8080";
}