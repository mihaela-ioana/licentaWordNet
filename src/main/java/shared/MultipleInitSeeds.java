package shared;

import java.util.Arrays;
import java.util.List;

public interface MultipleInitSeeds {
    List<String> posAdj = Arrays.asList("absolutely", "adorable", "amazing", "awesome", "active",
            "appealing", "beneficial", "brilliant", "beautiful", "brave", "calm", "cute", "clean", "charming",
            "divine", "ethical", "easy", "excellent", "efficient", "exciting", "fair", "fine", "friendly",
            "good", "great", "glowing", "hearty", "healthy", "intelligent", "ideal", "jovial", "kind", "lucky",
            "meaningful", "pleasant", "productive", "safe", "skilled", "successful", "well"); //40
    List<String> posSubst = Arrays.asList("accomplishment", "agreement", "aptitude", "achievement",
            "beauty", "benefit", "champion", "charm", "creativity", "divinity", "ethic", "energy", "fairness",
            "friend", "family", "heaven", "harmony", "heart", "honor", "honesty", "health", "hug", "imagination",
            "innovation", "intellectual", "joy", "knowledge", "nature", "paradise", "principle", "quality",
            "skill", "sun", "smile", "soul", "wealth", "good", "victory", "virtue", "truth");
    List<String> posVerbs = Arrays.asList("admire", "accept", "accomplish", "beautify", "believe", "bless",
            "bloom", "brighten", "calm", "care", "captivate", "clean", "charm", "comfort", "congratulate", "create",
            "cure","delight", "donate", "ease", "excite", "fix", "graduate", "glow", "harmonize", "honor", "help",
            "hug", "illuminate", "improve", "inspire", "love", "please", "progress", "repair", "satisfy", "support",
            "trust", "uplift", "understand", "welcome"); //41
    List<String> negAdj = Arrays.asList("angry", "anxious", "bad", "boring", "banal", "broken", "crazy",
            "cruel", "confused", "corrupt", "damaged", "deprived", "dirty", "dreadful", "despicable", "dishonest",
            "depressed", "disgusting", "evil", "failed", "foul", "greedy", "guilty", "hard", "hurtful", "harmful",
            "impossible", "insane", "ignorant", "misunderstood", "mean", "negative", "offensive", "rude", "sad",
            "scary", "stinky", "stressful", "threatening", "vindictive"); //40
    List<String> negSubst = Arrays.asList("anger", "apathy", "alarm", "bad", "cruelty", "creep", "confuse",
            "criminal", "damage", "disease", "distress", "depression", "evil", "failure", "fight", "fear", "greed",
            "guilt", "harm", "hurt", "ill", "injury", "junk", "mess", "monster", "moan", "nobody", "offense", "pain",
            "pessimist", "rejection", "revolution", "revenge", "sadness","scare", "smell", "stress", "sickness",
            "terror", "threat", "wound"); //40
    List<String> negVerbs = Arrays.asList("annoy", "adverse", "bore", "break", "cry", "collapse", "corrupt",
            "creep", "confuse", "damage", "die", "deny", "deprive", "damage", "disgust", "dishonor", "enrage", "fear",
            "frighten", "fail", "fight", "guilt", "harm", "hate", "hurt", "injure", "misunderstand", "negate", "offens",
            "oppress", "perturb", "reject", "revenge", "revolt", "repulse", "stress", "scare", "stink", "threat",
            "terrify", "upset", "wound"); //42
}
