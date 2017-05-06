package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.strategy.exceptions.InvalidTrumpfException;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.util.Set;

/**
 * Created by joelniklaus on 05.05.17.
 */
public class MLHelper {

	public static Instances loadArff(String filePath) {
		Instances data = null;
		try {
			data = new ConverterUtils.DataSource(filePath).getDataSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		data.setClassIndex(0);
		return data;
	}

	public static Instances buildSingleInstanceInstances(Instances train, Set<Card> availableCards) {
		Instances newInstances = new Instances(train);
		newInstances.delete();
		newInstances.setClassIndex(0);

		double[] instanceValues = new double[train.numAttributes()];
		int index = 0;

		// trump
		instanceValues[index] = 0;
		index++;

		// cards
		for (Card card : availableCards) {
			instanceValues[index] = train.attribute("card" + index).indexOfValue(card.toString());
			index++;
		}

		newInstances.add(new DenseInstance(1.0, instanceValues));

		return newInstances;
	}

	public static Mode predictTrumpf(Classifier classifier, Instances cards) throws InvalidTrumpfException {
		double predictedClass = 0;
		try {
			predictedClass = classifier.classifyInstance(cards.firstInstance());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return convertStringToMode(cards.classAttribute().value((int) predictedClass));
	}

	public static Mode convertStringToMode(String trumpf) throws InvalidTrumpfException {
		switch (trumpf) {
			case "HEARTS":
				return Mode.from(Trumpf.TRUMPF, Color.HEARTS);
			case "DIAMONDS":
				return Mode.from(Trumpf.TRUMPF, Color.DIAMONDS);
			case "CLUBS":
				return Mode.from(Trumpf.TRUMPF, Color.CLUBS);
			case "SPADES":
				return Mode.from(Trumpf.TRUMPF, Color.SPADES);
			case "OBEABE":
				return Mode.topDown();
			case "UNDEUFE":
				return Mode.bottomUp();
			case "SCHIEBE":
				return Mode.shift();
		}
		throw new InvalidTrumpfException("Trump not found.");
	}

}
