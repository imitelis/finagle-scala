// Names and Naming
case class Name.Bound(va: Var[Addr])

case class Name.Path(path: Path)

scheme!arg

inet!twitter.com:80

zk!myzkhost.mycompany.com:2181!/my/zk/path

twitter.com:8080

inet!twitter.com:8080

// Paths
/s/crawler

// Interpreting Paths With Delegation Tables
src     =>      dest

/s      =>      /s#/foo/bar

/s/crawler

/s#/foo/bar/crawler

/s#/*/bar       =>      /t/bah

/s#/foo/bar/baz

/s#/boo/bar/baz

/t/bah/baz

/$/namer/path..

/$/inet/localhost/8080

# delegation for /s
/s => /a      # prefer /a
    | ( /b    # or share traffic between /b and /c
      & /c
      );

/s => /a | (/b & /c);

/zk#    =>      /$/com.twitter.serverset;
/zk     =>      /zk#;
/s##    =>      /zk/zk.local.twitter.com:2181;
/s#     =>      /s##/prod;
/s      =>      /s#;

/s/crawler

1.      /s/crawler
2.      /s#/crawler
3.      /s##/prod/crawler
4.      /zk/zk.local.twitter.com:2181/prod/crawler
5.      /zk#/zk.local.twitter.com:2181/prod/crawler
6.      /$/com.twitter.serverset/zk.local.twitter.com:2181/prod/crawler

/s      =>      /s/prefix

/s/crawler
/s/prefix/crawler
/s/prefix/prefix/crawler
...

/s      =>      /s#/prefix

/s/crawler
/s#/prefix/crawler
...

/zk#  => /$/com.twitter.serverset;         (a)
/zk   => /zk#;                             (b)
/s##  => /zk/zk.local.twitter.com:2181;    (c)
/s#   => /s##/prod;                        (d)
/s    => /s#;                              (e)
/s#   => /s##/staging;                     (f)

    /s/crawler
(e) /s#/crawler
(f) /s##/staging/crawler
(c) /zk/zk.local.twitter.com:2181/staging/crawler
(b) /zk#/zk.local.twitter.com:2181/staging/crawler
(a) /$/com.twitter.serverset/zk.local.twitter.com:2181/staging/crawler

    /s/crawler
(e) /s#/crawler
(f) /s##/staging/crawler
  (c) /zk/zk.local.twitter.com:2181/staging/crawler
  (b) /zk#/zk.local.twitter.com:2181/staging/crawler
  (a) /$/com.twitter.serverset/zk.local.twitter.com:2181/staging/crawler
(d) /s##/prod/crawler
  (c) /zk/zk.local.twitter.com:2181/prod/crawler
  (b) /zk#/zk.local.twitter.com:2181/prod/crawler
  (a) /$/com.twitter.serverset/zk.local.twitter.com:2181/prod/crawler