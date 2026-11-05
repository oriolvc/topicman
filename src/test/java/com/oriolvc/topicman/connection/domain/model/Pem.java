package com.oriolvc.topicman.connection.domain.model;

/**
 * Certificates for domain tests, inline so they do no I/O: copies of {@code ca.pem} and
 * {@code other-ca.pem} in {@code src/test/resources/tls/}. Any valid certificate would do.
 */
final class Pem {

    static final String CA = """
            -----BEGIN CERTIFICATE-----
            MIIDKTCCAhGgAwIBAgIUKmdq+RaInm9m40qvv0QONJadkjEwDQYJKoZIhvcNAQEL
            BQAwGzEZMBcGA1UEAwwQVG9waWNNYW4gVGVzdCBDQTAgFw0yNjExMDQxNDU5MTJa
            GA8yMTI2MTAxMTE0NTkxMlowGzEZMBcGA1UEAwwQVG9waWNNYW4gVGVzdCBDQTCC
            ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMBx0Xs3B6CWtrOZvKgK/HqD
            XX6pAJGSBiHJsDxYC0Ew/TYBl4fA6lc00p1LK3ZkB3Rlw4FidZdpZF8PDltp5mVk
            EUzl0ldBz4mm1k2fUfPLvO/PxCW2JJ8MTFmU25xcd7N1yB8EZZTm05EHzmbwFH22
            /qJsWaydBpHDb3Zb8ekxHIPbauYADYDurjqW492TryZA1BeWcGNOpKBCIeZ01msr
            p6+pA+BrcAzvke9KDKJiJROtNB+6cPtzcl3y4j+6ojIKaPQ6avmw/tAkkGcXESm/
            3VUWRBEuAgoQhNYspOf3T0ZrP1zIrfdDIIlPryo6wEUuGTqZfxoPcMZd1Yhm6x8C
            AwEAAaNjMGEwHQYDVR0OBBYEFCBeHzs4N15jw78hahYgwPTVQubGMB8GA1UdIwQY
            MBaAFCBeHzs4N15jw78hahYgwPTVQubGMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0P
            AQH/BAQDAgEGMA0GCSqGSIb3DQEBCwUAA4IBAQA8Q/khdktnBwgqDdImyOBIEwA+
            wRxXaWVlTn7PAS4kK//7baRZ6rSBo1R4K1mSkzBhYUcTdVu85vuPg5DDnWUB4pw8
            WeNhC3cgJv4svj+45Z9fy4/Jv+In9Tmb76QDA3sN3qHBpkW6P5xZKk67T7Ls2k/U
            P/DipOuzRJAdHDMBkWJckYsq5KnC1c3Am/9Y4KegqU6imc73Ok9D7y538/GFW3S2
            ZIPO3f8O/Oxnnr/jNN3Mof2MY1gxyPLq2mTnXMetlSVX8/7gRcc0RS+dIk90Il4w
            Br3bBQKyQSD0a1ZuMWNpzazdrQquVz5DNaeoNIFoV9DslFYxW8LBPQEotBpk
            -----END CERTIFICATE-----
            """;

    static final String OTHER_CA = """
            -----BEGIN CERTIFICATE-----
            MIIDGzCCAgOgAwIBAgIUeZlZpsXKQHAAvQ6mRbw+AsSS78UwDQYJKoZIhvcNAQEL
            BQAwHDEaMBgGA1UEAwwRVW5yZWxhdGVkIFRlc3QgQ0EwIBcNMjYxMTA0MTQ1OTEz
            WhgPMjEyNjEwMTExNDU5MTNaMBwxGjAYBgNVBAMMEVVucmVsYXRlZCBUZXN0IENB
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvrV4q/st7I452HkuvAv/
            FKnhYEz3pG8eJk1qmWFIj6ws2X+nfCtyiNN2smcak4Mkdgkz1TZIDe3odwaoUu87
            y47JxJmERUuWQdN0QO0Lifw2z9PwVncdijK9IYwao2o+kxXTY0Lp0GVximmkB6pB
            QZRR3Bmi5tyGx0bdpHyQqh8epoKrVQPJCgcl5qkGlDjK7/ECUKWYgY8dCbuph4RZ
            SF6cWS/jHapt2Y4OJ3yDrqQQ8OeS23vDrJXkAKifCaqbU/ZMBK05M0P7D5DXbwRI
            QDqqkuFZ0FEmUbzI28UHqUW2x5lk/0sy4e3N+aE6Tdpl8x4b0e1sZF+bU+TWX5Co
            CwIDAQABo1MwUTAdBgNVHQ4EFgQUxDLksITwD8946xtF7ILivSYYOIcwHwYDVR0j
            BBgwFoAUxDLksITwD8946xtF7ILivSYYOIcwDwYDVR0TAQH/BAUwAwEB/zANBgkq
            hkiG9w0BAQsFAAOCAQEAM75tzmn9ce8uj9lHYolubGn2W3MX2L08pD7tyTIJ5jHQ
            4umxOkxQ77NFyMqpMkRnrEd7ZsdAFEiEM+hoobkJzlB91CDx+172bbomi1NH8SNJ
            lTRtnfinuUAxwaHsbGtjpuBc5srm7z+Zg5p72U3LXxHBWYEMAQjHikdHnusQwXIa
            3UjUP0Z1NoaaCBIieiACmxzwtZavcdtSrlBJDhWxufA7nZNEYZNjos7R67EiwW+F
            BBdwYJ7NdywolGWzBQSU06gjTj4838D+Kmn6nGVwy8O96f6B7tkDMB0aaiWCiXWe
            TXtEmN7bFdIHPuAiWqT0FJ9LERdmQLOE9E0xkE6bcQ==
            -----END CERTIFICATE-----
            """;

    /** Only its markers matter: the domain rejects any private key before parsing. */
    static final String PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC
            -----END PRIVATE KEY-----
            """;

    private Pem() {}
}
