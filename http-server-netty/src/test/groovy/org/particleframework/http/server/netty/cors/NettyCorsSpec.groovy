package org.particleframework.http.server.netty.cors

import org.particleframework.context.annotation.Requires
import org.particleframework.http.HttpRequest
import org.particleframework.http.HttpResponse
import org.particleframework.http.HttpStatus
import org.particleframework.http.server.netty.AbstractParticleSpec
import org.particleframework.http.annotation.Controller
import org.particleframework.http.annotation.Get

import static org.particleframework.http.HttpHeaders.*

class NettyCorsSpec extends AbstractParticleSpec {

    void "test non cors request"() {
        when:
        def response = rxClient.exchange('/test').blockingFirst()
        Set<String> headerNames = response.getHeaders().names()

        then:
        response.status == HttpStatus.NO_CONTENT
        response.contentLength == -1
        headerNames.empty

    }

    void "test cors request without configuration"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.GET('/test')
                           .header(ORIGIN, 'fooBar.com')
        ).blockingFirst()

        when:
        Set<String> headerNames = response.headers.names()

        then:
        response.status == HttpStatus.NO_CONTENT
        headerNames.empty
    }

    void "test cors request with a controller that returns map"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.GET('/test/arbitrary')
                        .header(ORIGIN, 'foo.com')
        ).blockingFirst()

        when:
        Set<String> headerNames = response.headers.names()

        then:
        response.status == HttpStatus.OK
        response.header(ACCESS_CONTROL_ALLOW_ORIGIN) == 'foo.com'
        response.header(VARY) == ORIGIN
        !headerNames.contains(ACCESS_CONTROL_MAX_AGE)
        !headerNames.contains(ACCESS_CONTROL_ALLOW_HEADERS)
        !headerNames.contains(ACCESS_CONTROL_ALLOW_METHODS)
        !headerNames.contains(ACCESS_CONTROL_EXPOSE_HEADERS)
        response.header(ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

    }

    void "test cors request with controlled method"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.GET('/test')
                        .header(ORIGIN, 'foo.com')
        ).blockingFirst()

        when:
        Set<String> headerNames = response.headers.names()

        then:
        response.status == HttpStatus.NO_CONTENT
        response.header(ACCESS_CONTROL_ALLOW_ORIGIN) == 'foo.com'
        response.header(VARY) == ORIGIN
        !headerNames.contains(ACCESS_CONTROL_MAX_AGE)
        !headerNames.contains(ACCESS_CONTROL_ALLOW_HEADERS)
        !headerNames.contains(ACCESS_CONTROL_ALLOW_METHODS)
        !headerNames.contains(ACCESS_CONTROL_EXPOSE_HEADERS)
        response.header(ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'
    }

    void "test cors request with controlled headers"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.GET('/test')
                        .header(ORIGIN, 'bar.com')
                        .header(ACCEPT, 'application/json')

        ).blockingFirst()

        when:
        Set<String> headerNames = response.headers.names()

        then:
        response.code() == HttpStatus.NO_CONTENT.code
        response.header(ACCESS_CONTROL_ALLOW_ORIGIN) == 'bar.com'
        response.header(VARY) == ORIGIN
        !headerNames.contains(ACCESS_CONTROL_MAX_AGE)
        !headerNames.contains(ACCESS_CONTROL_ALLOW_HEADERS)
        !headerNames.contains(ACCESS_CONTROL_ALLOW_METHODS)
        response.headers.getAll(ACCESS_CONTROL_EXPOSE_HEADERS) == ['x', 'y']
        !headerNames.contains(ACCESS_CONTROL_ALLOW_CREDENTIALS)
    }

    void "test cors request with invalid method"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.POST('/test', [:])
                        .header(ORIGIN, 'foo.com')

        ).onErrorReturn({ t -> t.response} ).blockingFirst()

        when:
        Set<String> headerNames = response.headers.names()

        then:
        response.code() == HttpStatus.FORBIDDEN.code
        headerNames == ['connection'] as Set

    }

    void "test cors request with invalid header"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.GET('/test')
                        .header(ORIGIN, 'bar.com')
                        .header(ACCESS_CONTROL_REQUEST_HEADERS, 'Foo, Accept')

        ).blockingFirst()

        expect: "it passes through because only preflight requests check allowed headers"
        response.code() == HttpStatus.NO_CONTENT.code

    }

    void "test preflight request with invalid header"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.OPTIONS('/test')
                        .header(ACCESS_CONTROL_REQUEST_METHOD, 'GET')
                        .header(ORIGIN, 'bar.com')
                        .header(ACCESS_CONTROL_REQUEST_HEADERS, 'Foo, Accept')

        ).onErrorReturn({ t -> t.response} ).blockingFirst()


        expect: "it fails because preflight requests check allowed headers"
        response.code() == HttpStatus.FORBIDDEN.code
    }

    void "test preflight request with invalid method"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.OPTIONS('/test')
                        .header(ACCESS_CONTROL_REQUEST_METHOD, 'POST')
                        .header(ORIGIN, 'foo.com')

        ).onErrorReturn({ t -> t.response} ).blockingFirst()

        expect:
        response.code() == HttpStatus.FORBIDDEN.code
    }

    void "test preflight request with controlled method"() {
        given:
        def response = rxClient.exchange(
                HttpRequest.OPTIONS('/test')
                        .header(ACCESS_CONTROL_REQUEST_METHOD, 'GET')
                        .header(ORIGIN, 'foo.com')
                        .header(ACCESS_CONTROL_REQUEST_HEADERS, 'Foo, Bar')

        ).blockingFirst()

        def headerNames = response.headers.names()

        expect:
        response.code() == HttpStatus.OK.code
        response.header(ACCESS_CONTROL_ALLOW_METHODS) == 'GET'
        response.headers.getAll(ACCESS_CONTROL_ALLOW_HEADERS) == ['Foo', 'Bar']
        !headerNames.contains(ACCESS_CONTROL_MAX_AGE)
        response.header(ACCESS_CONTROL_ALLOW_ORIGIN) == 'foo.com'
        response.header(VARY) == ORIGIN
        !headerNames.contains(ACCESS_CONTROL_EXPOSE_HEADERS)
        response.header(ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'
    }

    void "test preflight request with controlled headers"() {

        given:
        def response = rxClient.exchange(
                HttpRequest.OPTIONS('/test')
                        .header(ACCESS_CONTROL_REQUEST_METHOD, 'POST')
                        .header(ORIGIN, 'bar.com')
                        .header(ACCESS_CONTROL_REQUEST_HEADERS, 'Accept')
        ).blockingFirst()

        def headerNames = response.headers.names()

        expect:
        response.code() == HttpStatus.OK.code
        response.header(ACCESS_CONTROL_ALLOW_METHODS) == 'POST'
        response.headers.getAll(ACCESS_CONTROL_ALLOW_HEADERS) == ['Accept']
        response.header(ACCESS_CONTROL_MAX_AGE) == '150'
        response.header(ACCESS_CONTROL_ALLOW_ORIGIN) == 'bar.com'
        response.header(VARY) == ORIGIN
        response.headers.getAll(ACCESS_CONTROL_EXPOSE_HEADERS) == ['x', 'y']
        !headerNames.contains(ACCESS_CONTROL_ALLOW_CREDENTIALS)

    }

    Map<String, Object> getConfiguration() {
        ['particle.server.cors.enabled': true,
        'particle.server.cors.configurations.foo.allowedOrigins': ['foo.com'],
        'particle.server.cors.configurations.foo.allowedMethods': ['GET'],
        'particle.server.cors.configurations.foo.maxAge': -1,
        'particle.server.cors.configurations.bar.allowedOrigins': ['bar.com'],
        'particle.server.cors.configurations.bar.allowedHeaders': ['Content-Type', 'Accept'],
        'particle.server.cors.configurations.bar.exposedHeaders': ['x', 'y'],
        'particle.server.cors.configurations.bar.maxAge': 150,
        'particle.server.cors.configurations.bar.allowCredentials': false]
    }

    @Controller
    @Requires(property = 'spec.name', value = 'NettyCorsSpec')
    static class TestController {

        @Get('/')
        HttpResponse index() {
            HttpResponse.noContent()
        }

        @Get
        Map arbitrary() {
            [some: 'data']
        }
    }
}
