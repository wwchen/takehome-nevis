package com.nevis.search.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

/**
 * All errors come back as RFC 9457 `application/problem+json`.
 * Validation failures additionally carry an `errors` map of `field -> message`.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(ClientNotFoundException::class, DocumentNotFoundException::class)
    fun notFound(e: RuntimeException, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, e.message, req)

    @ExceptionHandler(DuplicateEmailException::class)
    fun conflict(e: DuplicateEmailException, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.CONFLICT, e.message, req)

    /** Unique-index race that slipped past the pre-check. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun integrity(e: DataIntegrityViolationException, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.CONFLICT, "Request conflicts with existing data", req)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun typeMismatch(e: MethodArgumentTypeMismatchException, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Parameter '${e.name}' has an invalid value '${e.value}'", req)

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.bindingResult.fieldErrors
            .groupBy({ snakeCase(it.field) }, { it.defaultMessage ?: "invalid" })
            .mapValues { (_, msgs) -> msgs.distinct().joinToString("; ") }
        val body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body failed validation").apply {
            title = "Bad Request"
            setProperty("errors", errors)
        }
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request)
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.parameterValidationResults.associate { r ->
            val name = r.methodParameter.parameterName ?: "param${r.methodParameter.parameterIndex}"
            snakeCase(name) to r.resolvableErrors.joinToString("; ") { it.defaultMessage ?: "invalid" }
        }
        val body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request parameters failed validation").apply {
            title = "Bad Request"
            setProperty("errors", errors)
        }
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request)
    }

    private fun problem(status: HttpStatus, detail: String?, req: HttpServletRequest): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail ?: status.reasonPhrase).apply {
            title = status.reasonPhrase
            instance = URI.create(req.requestURI)
        }

    private fun snakeCase(field: String): String =
        field.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}
