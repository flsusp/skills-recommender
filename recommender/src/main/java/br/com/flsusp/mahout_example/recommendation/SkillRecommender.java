package br.com.flsusp.mahout_example.recommendation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVParser;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class SkillRecommender {

	private static final int NUMBER_OF_RECOMMENDATIONS = 5;
	private final DataModel model;
	private final Map<Long, String> skillNamesById;
	private final UserBasedRecommender recommender;

	public SkillRecommender(File ratings, File skills) throws IOException, TasteException {
		this.model = new FileDataModel(ratings);
		this.skillNamesById = loadSkills(new CSVParser(new FileReader(skills)));

		UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
		UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
		recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
	}

	private Map<Long, String> loadSkills(CSVParser parser) throws IOException {
		Map<Long, String> result = new HashMap<>();
		for (String[] line : parser.getAllValues()) {
			result.put(Long.valueOf(line[1]), line[0]);
		}
		return result;
	}

	public List<String> recommendSkillsForUser(int userId) throws TasteException {
		List<RecommendedItem> recommendations = recommender.recommend(userId, NUMBER_OF_RECOMMENDATIONS,
				new IgnoreUnderRatedRecommendationsRescorer(model, userId), true);
		List<String> skillsToRecommend = new ArrayList<>();
		recommendations
				.forEach(recommendation -> skillsToRecommend.add(skillNamesById.get(recommendation.getItemID())));
		return skillsToRecommend;
	}
}

class IgnoreUnderRatedRecommendationsRescorer implements IDRescorer {

	private final Map<Long, Float> ratingBySkillForUser;

	public IgnoreUnderRatedRecommendationsRescorer(DataModel model, int userId) {
		ratingBySkillForUser = new HashMap<>();
		try {
			model.getPreferencesFromUser(userId).forEach(
					preference -> ratingBySkillForUser.put(preference.getItemID(), preference.getValue()));
		} catch (TasteException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double rescore(long skillId, double rating) {
		if (ratingBySkillForUser.containsKey(skillId)) {
			float originalRating = ratingBySkillForUser.get(skillId);
			if (originalRating >= rating)
				return 0;
		}
		return rating;
	}

	@Override
	public boolean isFiltered(long rating) {
		return rating < 2;
	}
}
