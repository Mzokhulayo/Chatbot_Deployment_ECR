package com.example.chatbot.repository;

import com.example.chatbot.model.Otp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class OtpRepository {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public void saveOtp(Otp otp) {
        String sql = "insert into otp (phone_number, otp, expiration_time) values (?, ?, ?)";
        jdbcTemplate.update(sql, otp.getPhoneNumber(), otp.getOtp(), otp.getExpirationTime());
    }


    public Optional<Otp> findByPhoneNumber(String phoneNumber) {
        String sql = "select * from otp where phone_number = ?";
        return jdbcTemplate.query(sql, new Object[]{phoneNumber}, new OtpRowMapper()).stream().findFirst();
    }

    private static class OtpRowMapper implements RowMapper<Otp> {
        @Override
        public Otp mapRow(ResultSet rs, int rowNum) throws SQLException {
            Otp otp = new Otp();
            otp.setPhoneNumber(rs.getString("phone_number"));
            otp.setOtp(rs.getString("otp"));
            otp.setExpirationTime(rs.getTimestamp("expiration_time").toLocalDateTime());
            return otp;
        }
    }
}
