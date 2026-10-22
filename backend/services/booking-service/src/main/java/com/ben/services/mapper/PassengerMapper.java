package com.ben.services.mapper;

import com.ben.common_lib.payload.request.PassengerRequest;
import com.ben.common_lib.payload.response.PassengerResponse;
import com.ben.services.model.Passenger;

public class PassengerMapper {

    public static Passenger toEntity(PassengerRequest request) {
        return Passenger.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .passportNumber(request.getPassportNumber())
                .nationality(request.getNationality())
                .frequentFlyerNumber(request.getFrequentFlyerNumber())
                .requiresWheelchairAssistance(request.getRequiresWheelchairAssistance())
                .dietaryPreferences(request.getDietaryPreferences())
                .medicalConditions(request.getMedicalConditions())
                .build();
    }

    public static PassengerResponse toResponse(Passenger passenger) {
        return PassengerResponse.builder()
                .id(passenger.getId())
                .firstName(passenger.getFirstName())
                .lastName(passenger.getLastName())
                .email(passenger.getEmail())
                .phone(passenger.getPhone())
                .dateOfBirth(passenger.getDateOfBirth())
                .gender(passenger.getGender())
                .passportNumber(passenger.getPassportNumber())
                .nationality(passenger.getNationality())
                .frequentFlyerNumber(passenger.getFrequentFlyerNumber())
                .primaryUserId(passenger.getPrimaryUserId())
                .requiresWheelchairAssistance(passenger.getRequiresWheelchairAssistance())
                .dietaryPreferences(passenger.getDietaryPreferences())
                .medicalConditions(passenger.getMedicalConditions())
                .isActive(passenger.getIsActive())
                .age(passenger.getAge())
                .isAdult(passenger.isAdult())
                .fullName(passenger.getFullName())
                .createdAt(passenger.getCreatedAt())
                .updatedAt(passenger.getUpdatedAt())
                .build();
    }
}
