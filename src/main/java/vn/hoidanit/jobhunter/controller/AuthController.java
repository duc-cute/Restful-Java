package vn.hoidanit.jobhunter.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.*;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.dto.LoginDTO;
import vn.hoidanit.jobhunter.domain.dto.ResLoginDTO;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.SercurityUtil;
import vn.hoidanit.jobhunter.util.anotation.ApiMessage;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SercurityUtil sercurityUtil;

    private final UserService userService;

    @Value("${duccute.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder,
                          SercurityUtil sercurityUtil,
                          UserService userService
                          ) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.sercurityUtil = sercurityUtil;
        this.userService = userService;

    }

    @PostMapping("/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
//        Nạp input username ,password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginDTO.getUserName(),loginDTO.getPassword());

//        Tiến hành xác thực
        Authentication authentication =authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // set thông tin người dùng đăng nhập vào context (có thể sử dụng sau này)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User userInDB = this.userService.handleGetUserByUserName(loginDTO.getUserName());

        ResLoginDTO res = new ResLoginDTO();
        if(userInDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    userInDB.getId(),
                    userInDB.getEmail(),
                    userInDB.getName()
            );
            res.setUser(userLogin);
        }
        String access_token = this.sercurityUtil.createAccessToken(authentication,res.getUser());
        res.setAccessToken(access_token);

        //create token
        String refreshToken = sercurityUtil.createRefreshToken(loginDTO.getUserName(),res);

        //update in db
        this.userService.updateUserToken(refreshToken, loginDTO.getUserName());

        //set cookie
        ResponseCookie resCookie = ResponseCookie
                .from("refresh_token",refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();



        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,resCookie.toString()).body(res);
    }
    @GetMapping("/account")
    @ApiMessage("Fetch account")
    public ResponseEntity<ResLoginDTO.UserLogin> getAccount() {
        String email = SercurityUtil.getCurrentUserLogin().isPresent() ?
                SercurityUtil.getCurrentUserLogin().get() : "";
        User currentUser = this.userService.handleGetUserByUserName(email);

        ResLoginDTO.UserLogin userLgin = new ResLoginDTO.UserLogin();
        if(currentUser != null) {
            userLgin.setId(currentUser.getId());
            userLgin.setName(currentUser.getName());
            userLgin.setEmail(currentUser.getEmail());

        }
        return ResponseEntity.ok(userLgin);
    }
}
