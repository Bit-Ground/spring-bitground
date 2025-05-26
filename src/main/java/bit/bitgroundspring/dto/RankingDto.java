package bit.bitgroundspring.dto;

import lombok.Data;

@Data
public class RankingDto {
   private int userId; //유저 ID
   private int seasonId; //시즌ID
   private int ranks; //랭킹
   private float totalValue; //총자산
   private String profileImage;   // 유저 프로필
   private String tier;           // 이전시즌 등급
}
