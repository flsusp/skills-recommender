package br.com.flsusp.mahout_example.recommendation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;

public class Main {

	public static void main(String[] args) throws IOException, TasteException {
		SkillRecommender recommender = new SkillRecommender(new File(SkillRecommender.class.getClassLoader()
				.getResource("skill-ratings.csv").getPath()), new File(SkillRecommender.class.getClassLoader()
				.getResource("skills.csv").getPath()));
		List<String> recommendedSkills = recommender.recommendSkillsForUser(16591); // TODO: Put your user ID here
		System.out.println(recommendedSkills);
	}
}
