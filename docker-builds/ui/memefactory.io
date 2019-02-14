server {
  listen 80 default_server;
#  server_name localhost ec2-52-42-54-97.us-west-2.compute.amazonaws.com memefactory.qa.district0x.io;

  auth_basic "Restricted Content";
  auth_basic_user_file /etc/nginx/memefactory.passwd;

  location ~ /(contracts|images|js|css|fonts|assets)(.*)$ {
    auth_basic off;
    try_files $uri @static;
  }

  location / {
    try_files $uri @static;
  }

  location @static {
    # root /usr/share/nginx/html/build/memefactory;
    root memefactory/resource/public;
  }
  
  location = /X0X.html {
    root /usr/share/nginx/html/;
    internal;
  }
  
   # redirect server error pages to the static error page
  error_page 400 401 402 403 404 405 406 407 408 409 410 411 412 413 414 415 416 417 418 420 422 423 424 426 428 429 431 444 449 450 451 500 501 502 503 504 505 506 507 508 509 510 511 /X0X.html;

}
