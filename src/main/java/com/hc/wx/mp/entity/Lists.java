package com.hc.wx.mp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Lists {
    private String question;
    private String answer;


    @Override
    public String toString() {

//        if (answer.isEmpty()&&answer.contains(question)){
//            return  ""+ answer + '\'';
//        }
        return question +"   " + answer + '\'';

    }
}
