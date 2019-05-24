package io.seqera.watchtower.controller

import grails.gorm.transactions.Transactional
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.AccessRefreshToken
import io.micronaut.security.token.jwt.validator.JwtTokenValidator
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import io.seqera.watchtower.Application
import io.seqera.watchtower.domain.auth.User
import io.seqera.watchtower.domain.auth.UserRole
import io.seqera.watchtower.util.AbstractContainerBaseTest
import io.seqera.watchtower.util.DomainCreator
import spock.lang.Ignore

import javax.inject.Inject

@MicronautTest(application = Application.class)
@Transactional
@Ignore("throws 'IllegalStateException: state should be: open' when executing all tests")
class LoginControllerTest extends AbstractContainerBaseTest {

    @Inject
    JwtTokenValidator tokenValidator

    @Inject
    @Client('/')
    RxHttpClient client

    void 'login with valid credentials (username and authToken) for a user'() {
        given: "a user"
        User user = new DomainCreator().createUser(firstName: 'User', lastName: 'Userson', avatar: 'http://image.com')

        and: "grant a role to the user"
        UserRole userRole = new DomainCreator().createUserRole(user: user)

        when: "do the login request attaching username and authToken as credentials"
        HttpRequest request = HttpRequest.create(HttpMethod.POST, '/login')
                                         .accept(MediaType.APPLICATION_JSON_TYPE)
                                         .body(new UsernamePasswordCredentials(user.email, user.authToken))
        HttpResponse<AccessRefreshToken> response = client.toBlocking().exchange(request, AccessRefreshToken)

        then:
        response.status == HttpStatus.OK
        response.body.isPresent()
        response.body.get().accessToken
        response.body.get().refreshToken

        when:
        String accessToken = response.body.get().accessToken
        Authentication authentication = Flowable.fromPublisher(tokenValidator.validateToken(accessToken)).blockingFirst()

        then:
        authentication.attributes
        authentication.attributes.roles == [userRole.role.authority]
        authentication.attributes.iss
        authentication.attributes.exp
        authentication.attributes.iat

        authentication.attributes.firstName == user.firstName
        authentication.attributes.lastName == user.lastName
        authentication.attributes.avatar == user.avatar
    }

    void 'try to login without supplying credentials'() {
        when: "do the login request"
        HttpRequest request = HttpRequest.create(HttpMethod.POST, '/login')
                                         .accept(MediaType.APPLICATION_JSON_TYPE)
        client.toBlocking().exchange(request)

        then: "the server responds BAD REQUEST"
        HttpClientResponseException e = thrown(HttpClientResponseException)
        e.status == HttpStatus.BAD_REQUEST
    }

    void 'try to login supplying a bad combination of credentials (username and authToken)'() {
        given: 'a user'
        User user = new DomainCreator().createUser()

        when: "do the login request attaching a bad authToken"
        HttpRequest request = HttpRequest.create(HttpMethod.POST, '/login')
                                          .accept(MediaType.APPLICATION_JSON_TYPE)
                                          .body(new UsernamePasswordCredentials(user.email, 'badToken'))
        client.toBlocking().exchange(request, AccessRefreshToken)

        then: "the server responds UNAUTHORIZED"
        HttpClientResponseException e = thrown(HttpClientResponseException)
        e.status == HttpStatus.UNAUTHORIZED
    }

}