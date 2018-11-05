package com.example.demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.Principal;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.demo.GENDER.MALE;

@SpringBootApplication
@RestController // = @Controller en @ResponseBody )
public class DemoApplication {
    protected final Log logger = LogFactory.getLog(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    /********************************************
       Methods for get, post, put, patch, delete
     ********************************************/

    // curl http://localhost:8080/getmapping
    @GetMapping("/getmapping")
    public String testGetMapping(){
        return "getmapping";
    }

    // same as previous, but with RequestMapping
    // curl http://localhost:8080/getmapping2
    @RequestMapping(method = RequestMethod.GET, path="/getmapping2")
    public String testGetMapping2(){
        return "getmapping2";
    }

    // curl -X POST http://localhost:8080/postmapping
    @PostMapping("/postmapping")
    public String testPostMapping(){
        return "postmapping";
    }

    // curl -X PATCH http://localhost:8080/patchmapping
    @PatchMapping("/patchmapping")
    public String testPatchMapping(){
        return "patchmapping";
    }

    // curl -X PUT http://localhost:8080/putmapping
    @PutMapping("/putmapping")
    public String testPutMapping(){
        return "putmapping";
    }

    // curl -X DELETE http://localhost:8080/deletemapping
    @DeleteMapping("/deletemapping")
    public String testDeleteMapping(){
        return "deletemapping";
    }

    /********************************************
     Pathvariables
     ********************************************/
    // curl http://localhost:8080/owners/21/pets/31
    @GetMapping("/owners/{ownerId}/pets/{petId}")
    public Pet findPetById(@PathVariable long ownerId,  @PathVariable long petId) {
        return new Pet("Boof", MALE);
    }

    // gebruik reg expressies voor pathvariables
    // curl http://localhost:8080/jarinfo/spring-web-3.0.5.jar
    @GetMapping("/jarinfo/{name:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{ext:\\.[a-z]+}")
    public String handle(@PathVariable String name, @PathVariable String version, @PathVariable String ext) {
        return String.format("name:%s, version:%s, ext:%s", name, version, ext);
    }


    /********************************************
     RequestParam
     ********************************************/
    // curl "http://localhost:8080/getwithparams?name=Petsname&gender=MALE"
    @GetMapping("/getwithparams")
    public Pet setupForm2(
            @RequestParam(value = "name", required = true) String name,
            @RequestParam(value = "gender", required = true) GENDER gender
            ) {
        return new Pet(name, gender);
    }

    // curl "http://localhost:8080/getwithparams2?name=Petsname&gender=MALE"
    @GetMapping("/getwithparams2")
    public Pet setupForm3(@RequestParam Map params) {
        String name = (String)params.get("name");
        GENDER gender = GENDER.valueOf((String)params.get("gender"));
        return new Pet(name, gender);
    }

    /********************************************
     Validations
     ********************************************/

    // when validation fails, the default validation handler will handle the request (defined in CustomRestExceptionHandler)
    // curl -d "{"""name""":"""pet""","""gender""":"""MALE"""}" -H "Content-Type: application/json" http://localhost:8080/pets2
    // curl -d "{}" -H "Content-Type: application/json" http://localhost:8080/pets2
    @PostMapping(path = "/pets2", consumes = "application/json")
    public ResponseEntity<Pet> addPet2(@Valid @RequestBody Pet pet) {
        return ResponseEntity.ok(pet);
    }

    // same example, but when the validation fails, the binding result will be injected
    // curl -d "{"""name""":"""pet""","""gender""":"""MALE"""}" -H "Content-Type: application/json" http://localhost:8080/pets
    // curl -d "{}" -H "Content-Type: application/json" http://localhost:8080/pets
    @PostMapping(path = "/pets", consumes = "application/json")
    public ResponseEntity<Pet> addPet(@Valid @RequestBody Pet pet, BindingResult bindingResult) {
        if (bindingResult.hasErrors()){
            String errorMessage = bindingResult.getAllErrors().stream().map(e -> e.toString()).collect(Collectors.joining(","));
            logger.error("Got the following validation error:"+errorMessage);
            throw new MyValidationException(bindingResult);
        }
        return ResponseEntity.ok(pet);
    }

    // same example, but when the validation fails, the errors will be injected
    // curl -d "{"""name""":"""pet""","""gender""":"""MALE"""}" -H "Content-Type: application/json" http://localhost:8080/pets3
    // curl -d "{}" -H "Content-Type: application/json" http://localhost:8080/pets3
    @PostMapping(path = "/pets3", consumes = "application/json")
    public ResponseEntity<Pet> addPet3(@Valid @RequestBody Pet pet, Errors errors) {
       if (errors.hasErrors()){
            String errorMessage = errors.getAllErrors().stream().map(e -> e.toString()).collect(Collectors.joining(","));
            logger.error("Got the following validation error:"+errorMessage);
            throw new MyValidationException(errors);
        }
        return ResponseEntity.ok(pet);
    }

    // Handles MyValidationException exceptions, only for this RestController
    @ExceptionHandler
    public ResponseEntity<String> handle(MyValidationException ex) {
        return ResponseEntity.badRequest().body("The following errors are found: "+ex.getBindingErrorMessage());
    }

    /*
      Handle application exception.
      The ExceptionHandler for MyAppException is on the controller advice, which is shared for all RestControllers
     */
    // curl "http://localhost:8080/apperror"
    @GetMapping("/apperror")
    public String getAppError(){
        throw new MyAppException("my error");
    }

    /*
      Using MatrixVariable
      to use this example, see https://sandeepcode.wordpress.com/tag/spring-boot/
     */

    // curl http://localhost:8080/matrixexample/42;q=11;r=12/pets/21;q=22;s=23
    @GetMapping("/matrixexample/{ownerId}/pets/{petId}")
    public String findPet(
            @MatrixVariable MultiValueMap<String, String> matrixVars,
            @MatrixVariable(pathVar="petId") MultiValueMap<String, String> petMatrixVars) {
        // matrixVars: ["q" : [11,22], "r" : 12, "s" : 23]
        // petMatrixVars: ["q" : 22, "s" : 23]
        return "ok";
    }

    /*
      Using MultipartFile
     */

    @PostMapping("/upload")
    public String handleFormUpload(@RequestParam("name") String name,
                                   @RequestParam("file") MultipartFile file) throws IOException {

        if (!file.isEmpty()) {
            byte[] bytes = file.getBytes();
            // store the bytes somewhere
            return "redirect:uploadSuccess";
        }
        return "redirect:uploadFailure";
    }

    /*
      Possible injected fields
     */

    // use browser: "http://localhost:8080/injectdemo"
    @GetMapping(path="/injectdemo")
    public Map<String, String> getTestInjectDemo(
            WebRequest webRequest,
            NativeWebRequest nativeWebRequest,
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            HttpSession httpSession,
            Principal principal,
            HttpMethod httpMethod,
            Locale locale,
            TimeZone timeZone,
            ZoneId zoneId,
            @RequestHeader("Accept-Encoding") String encoding,
            @CookieValue("JSESSIONID") String cookie

    ){
        Map<String, String> result = new HashMap<>();
        result.put("webRequest", webRequest.toString());
        result.put("nativeWebRequest", nativeWebRequest.toString());
        result.put("servletRequest", servletRequest.toString());
        result.put("servletResponse", servletResponse.toString());
        result.put("httpSession", httpSession.toString());
        result.put("principal", principal==null?null:principal.toString());
        result.put("httpMethod", httpMethod.toString());
        result.put("locale", locale.toString());
        result.put("timeZone", timeZone.toString());
        result.put("zoneId", zoneId.toString());
        result.put("encoding", encoding);
        result.put("cookie", cookie);
        return result;
    }

     /*
      Async call
     */
    // curl http://localhost:8080/getasync
    @GetMapping("/getasync")
    @ResponseBody
    public DeferredResult<String> getasync() {
        DeferredResult<String> deferredResult = new DeferredResult<String>();
        new Thread(() -> {
            sleep(5000);
            deferredResult.setResult("getasync");
        }).start();
        // Save the deferredResult somewhere..
        return deferredResult;
    }

    /*
      Use ResponseBodyEmitter
     */
    // curl http://localhost:8080/httpstream
    @GetMapping(path="/httpstream")
    public ResponseBodyEmitter handleHttpStream() {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        new Thread(() -> {
            try {
                sleep(1000);
                emitter.send("Hello once");
                sleep(1000);
                emitter.send("Hello again");
                sleep(1000);
                emitter.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return emitter;
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}


class MyValidationException extends RuntimeException{
    private Errors errors;
    public MyValidationException(Errors errors){
        this.errors = errors;
    }
    String getBindingErrorMessage(){
        return errors.toString();
    }
}

class MyAppException extends RuntimeException{
    private String error;
    public MyAppException(String error){
        this.error = error;
    }
    String getErrorMessage(){
        return error;
    }
}

/*
 Catch alle validation exceptions
 */
// @ControllerAdvice(annotations = RestController.class)  // Target all Controllers annotated with @RestController
// @ControllerAdvice("org.example.controllers") // Target all Controllers within specific packages
// @ControllerAdvice(assignableTypes = {ControllerInterface.class, AbstractController.class}) // Target all Controllers assignable to specific classes
@ControllerAdvice
class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex, final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
        logger.info("Got a validation exception");
        //
        final List<String> errors = new ArrayList<String>();
        for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        return handleExceptionInternal(ex, errors, headers, HttpStatus.BAD_REQUEST, request);
    }

    // Handles MyAppException exceptions for all controllers
    @ExceptionHandler
    public ResponseEntity<String> handle(MyAppException ex) {
        return ResponseEntity.badRequest().body("Application error : "+ex.getErrorMessage());
    }

}

class Pet{
    @NotNull(message = "Name cannot be empty")
    public String name;
    @NotNull(message = "Gender cannot be empty")
    public GENDER gender;

    public Pet() {}

    public Pet(String name, GENDER gender) {
        this.name = name;
        this.gender = gender;
    }
}

enum GENDER{
    MALE,
    FEMALE
}

