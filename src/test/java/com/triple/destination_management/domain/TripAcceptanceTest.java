package com.triple.destination_management.domain;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triple.destination_management.domain.trip.dto.TripRequest;
import com.triple.destination_management.domain.trip.dto.TripResponse;
import com.triple.destination_management.domain.user.constants.Auth;
import com.triple.destination_management.domain.user.entity.User;
import com.triple.destination_management.global.config.security.jwt.JwtProperties;
import com.triple.destination_management.global.config.security.jwt.JwtProvider;
import com.triple.destination_management.global.constants.ResponseCode;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TripAcceptanceTest {

	private final MockMvc mvc;

	private final ObjectMapper objectMapper;

	private final JwtProvider jwtProvider;

	private final DataSource dataSource;

	private DateTimeFormatter formatter;

	private String token;

	public TripAcceptanceTest(
		@Autowired MockMvc mvc,
		@Autowired ObjectMapper objectMapper,
		@Autowired JwtProvider jwtProvider,
		@Autowired DataSource dataSource
	) {
		this.mvc = mvc;
		this.objectMapper = objectMapper;
		this.jwtProvider = jwtProvider;
		this.dataSource = dataSource;
	}

	@BeforeAll
	public void init() {
		User user = User.builder()
			.id(1L)
			.username("gyunam")
			.password("1234")
			.name("김규남")
			.role(Auth.ROLE_USER)
			.build();

		token = jwtProvider.createAccessToken(user);
		formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd a HH:mm:ss");

		try (Connection conn = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(conn, new ClassPathResource("/h2/data.sql"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	@DisplayName("# [1-1]-[POST] 여행 등록하기")
	void registerTrip() throws Exception {
		// given
		LocalDateTime startDate = LocalDateTime.now().plusDays(5);
		LocalDateTime endDate = LocalDateTime.now().plusDays(10);
		TripRequest tripRequest = getTripRequest(startDate, endDate, 5L);

		// when & then
		mvc.perform(post("/api/v1/trips")
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is2xxSuccessful())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.startDate").value(tripRequest.getStartDate().format(formatter)))
			.andExpect(jsonPath("$.data.endDate").value(tripRequest.getEndDate().format(formatter)))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
		;
	}

	@Test
	@DisplayName("# [1-2]-[POST] 날짜를 미입력 후 여행 등록하기")
	void registerTripWithoutDate() throws Exception {
		// given
		TripRequest tripRequest = getTripRequest(null, null, 1L);

		// when & then
		mvc.perform(post("/api/v1/trips")
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_ERROR.getCode()))
			.andExpect(jsonPath("$.message").value("날짜를 입력해주세요."))
		;
	}

	@Test
	@DisplayName("# [1-3]-[POST] 날짜를 잘못 입력 후 여행 등록하기")
	void registerTripWithWrongDate() throws Exception {
		// given
		LocalDateTime startDate = LocalDateTime.now().plusDays(-1);
		LocalDateTime endDate = LocalDateTime.now().plusDays(-1);
		TripRequest tripRequest = getTripRequest(startDate, endDate, 1L);

		// when & then
		mvc.perform(post("/api/v1/trips")
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_ERROR.getCode()))
			.andExpect(jsonPath("$.message").value("올바른 날짜를 입력해주세요."))
		;
	}

	@Test
	@DisplayName("# [1-4]-[POST] 도시 아이디 미입력 후 여행 등록하기")
	void registerTripWithoutTownId() throws Exception {
		// given
		LocalDateTime startDate = LocalDateTime.now().plusDays(5);
		LocalDateTime endDate = LocalDateTime.now().plusDays(10);
		TripRequest tripRequest = TripRequest.builder().townId(null).startDate(startDate).endDate(endDate).build();

		// when & then
		mvc.perform(post("/api/v1/trips")
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_ERROR.getCode()))
			.andExpect(jsonPath("$.message").value("목적지 아이디를 입력해주세요."))
		;
	}

	@Test
	@DisplayName("# [1-5]-[POST] 토큰 없이 여행 등록하기")
	void registerTripWithoutToken() throws Exception {
		// given
		LocalDateTime startDate = LocalDateTime.now().plusDays(5);
		LocalDateTime endDate = LocalDateTime.now().plusDays(10);
		TripRequest tripRequest = getTripRequest(startDate, endDate, 1L);

		// when & then
		mvc.perform(post("/api/v1/trips")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.ACCESS_DENIED.getMessage()))
		;
	}

	@Test
	@DisplayName("# [1-6]-[POST] 잘못된 토큰으로 여행 등록하기")
	void registerTripWithWrongToken() throws Exception {
		// given
		LocalDateTime startDate = LocalDateTime.now().plusDays(5);
		LocalDateTime endDate = LocalDateTime.now().plusDays(10);
		TripRequest tripRequest = getTripRequest(startDate, endDate, 1L);

		// when & then
		mvc.perform(post("/api/v1/trips")
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + "wrong token")
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.ACCESS_DENIED.getMessage()))
		;
	}

	@Test
	@DisplayName("# [2-1]-[PUT] 여행 수정하기")
	void modifyTrip() throws Exception {
		// given
		long tripId = 3L;
		LocalDateTime startDate = LocalDateTime.now().plusMonths(1);
		LocalDateTime endDate = LocalDateTime.now().plusMonths(2);
		TripRequest tripRequest = getTripRequest(startDate, endDate, 1L);

		// when & then
		mvc.perform(put("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is2xxSuccessful())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.id").value(tripId))
			.andExpect(jsonPath("$.data.startDate").value(tripRequest.getStartDate().format(formatter)))
			.andExpect(jsonPath("$.data.endDate").value(tripRequest.getEndDate().format(formatter)))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
		;
	}

	@Test
	@DisplayName("# [2-2]-[PUT] 날짜를 미입력 후 여행 수정하기")
	void modifyTripWithoutDate() throws Exception {
		// given
		long tripId = 1L;
		TripRequest tripRequest = getTripRequest(null, null, 1L);

		// when & then
		mvc.perform(put("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_ERROR.getCode()))
			.andExpect(jsonPath("$.message").value("날짜를 입력해주세요."))
		;
	}

	@Test
	@DisplayName("# [2-3]-[PUT] 날짜를 잘못입력 후 여행 수정하기")
	void modifyTripWithWrongDate() throws Exception {
		// given
		long tripId = 1L;
		LocalDateTime startDate = LocalDateTime.now().plusDays(-1);
		LocalDateTime endDate = LocalDateTime.now().plusDays(-1);
		TripRequest tripRequest = getTripRequest(startDate, endDate, 1L);

		// when & then
		mvc.perform(put("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_ERROR.getCode()))
			.andExpect(jsonPath("$.message").value("올바른 날짜를 입력해주세요."))
		;
	}

	@Test
	@DisplayName("# [2-4]-[PUT] 도시 아이디 미입력 후 여행 수정하기")
	void modifyTripWithoutTownId() throws Exception {
		// given
		long tripId = 1L;
		LocalDateTime startDate = LocalDateTime.now().plusDays(5);
		LocalDateTime endDate = LocalDateTime.now().plusDays(10);
		TripRequest tripRequest = TripRequest.builder().townId(null).startDate(startDate).endDate(endDate).build();

		// when & then
		mvc.perform(put("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_ERROR.getCode()))
			.andExpect(jsonPath("$.message").value("목적지 아이디를 입력해주세요."))
		;
	}

	@Test
	@DisplayName("# [2-5]-[PUT] 토큰 없이 여행 수정하기")
	void modifyTripWithoutToken() throws Exception {
		// given
		long tripId = 1L;
		LocalDateTime startDate = LocalDateTime.now().plusDays(5);
		LocalDateTime endDate = LocalDateTime.now().plusDays(10);
		TripRequest tripRequest = getTripRequest(startDate, endDate, 1L);

		// when & then
		mvc.perform(put("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.ACCESS_DENIED.getMessage()))
		;
	}

	@Test
	@DisplayName("# [2-6]-[PUT] 잘못된 토큰으로 여행 수정하기")
	void modifyTripWithWrongToken() throws Exception {
		// given
		long tripId = 1L;
		LocalDateTime startDate = LocalDateTime.now().plusDays(5);
		LocalDateTime endDate = LocalDateTime.now().plusDays(10);
		TripRequest tripRequest = getTripRequest(startDate, endDate, 1L);

		// when & then
		mvc.perform(put("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + "wrong token")
			.content(objectMapper.writeValueAsString(tripRequest)))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.ACCESS_DENIED.getMessage()))
		;
	}

	@Test
	@DisplayName("# [3-1]-[DELETE] 여행 삭제하기")
	void removeTrip() throws Exception {
		// given
		long tripId = 1L;

		// when & then
		mvc.perform(delete("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token))
			.andExpect(status().is2xxSuccessful())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.data").value(tripId))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
		;
	}

	@Test
	@DisplayName("# [3-2]-[DELETE] 토큰 없이 여행 삭제하기")
	void removeTripWithoutToken() throws Exception {
		// given
		long tripId = 1L;

		// when & then
		mvc.perform(delete("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.ACCESS_DENIED.getMessage()))
		;
	}

	@Test
	@DisplayName("# [3-3]-[DELETE] 잘못된 토큰으로 여행 삭제하기")
	void removeTripWithWrongToken() throws Exception {
		// given
		long tripId = 1L;

		// when & then
		mvc.perform(delete("/api/v1/trips/" + tripId)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + "wrong token")
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.ACCESS_DENIED.getMessage()))
		;
	}

	@Test
	@DisplayName("# [4-1]-[GET] 단일 여행 조회하기")
	void findTrip() throws Exception {
		// given
		long tripId = 1L;

		// when & then
		mvc.perform(get("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + token))
			.andExpect(status().is2xxSuccessful())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.data.id").value(tripId))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
		;
	}

	@Test
	@DisplayName("# [4-2]-[GET] 토큰 없이 단일 여행 조회하기")
	void findTripWithoutToken() throws Exception {
		// given
		long tripId = 1L;

		// when & then
		mvc.perform(get("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.ACCESS_DENIED.getMessage()))
		;
	}

	@Test
	@DisplayName("# [4-3]-[GET] 잘못된 토큰으로 단일 여행 조회하기")
	void findTripWithWrongToken() throws Exception {
		// given
		long tripId = 1L;

		// when & then
		mvc.perform(get("/api/v1/trips/" + tripId)
			.contentType(MediaType.APPLICATION_JSON)
			.header(JwtProperties.JWT_ACCESS_HEADER, JwtProperties.TOKEN_PREFIX + "wrong token"))
			.andExpect(status().is4xxClientError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
			.andExpect(jsonPath("$.message").value(ResponseCode.ACCESS_DENIED.getMessage()))
		;
	}

	private TripRequest getTripRequest(
		LocalDateTime startDate,
		LocalDateTime endDate,
		Long townId
	) {
		return TripRequest.builder()
			.startDate(startDate)
			.endDate(endDate)
			.townId(townId)
			.build();
	}
}
