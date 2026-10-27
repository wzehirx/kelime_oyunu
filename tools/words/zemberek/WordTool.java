import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import zemberek.core.turkish.RootAttribute;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;

/**
 * Zemberek 0.17.1 (Apache-2.0) ile sözcük çözümleme aracı. Politika (hangi sözcük kabul edilir)
 * burada DEĞİL, build_wordlists.py içindedir; bu araç yalnızca ham çözümlemeyi yazar.
 *
 *   WordTool lexicon <cikti.tsv>
 *       Zemberek sözlüğündeki her madde: lemma, root, birincil POS, ikincil POS, öznitelikler.
 *   WordTool analyze <girdi.txt> <cikti.tsv>
 *       Girdideki her sözcük (satır başına bir) için her çözümleme bir satır:
 *       sözcük, lemma, root, birincil POS, ikincil POS, informal(0/1).
 *       Çözümlemesi olmayan sözcük için tek satır: sözcük, "-".
 */
public class WordTool {
    public static void main(String[] a) throws Exception {
        if (a.length == 2 && a[0].equals("lexicon")) {
            lexicon(a[1]);
        } else if (a.length == 3 && a[0].equals("analyze")) {
            analyze(a[1], a[2]);
        } else {
            System.err.println("kullanim: WordTool lexicon <cikti> | WordTool analyze <girdi> <cikti>");
            System.exit(2);
        }
    }

    private static void lexicon(String out) throws IOException {
        List<String> rows = new ArrayList<>();
        for (DictionaryItem d : RootLexicon.getDefault().getAllItems()) {
            String attrs = d.attributes.stream().map(Enum::toString).sorted().collect(Collectors.joining(","));
            rows.add(String.join("\t", d.lemma, d.root, d.primaryPos.toString(),
                    d.secondaryPos.toString(), attrs));
        }
        Files.write(Paths.get(out), rows, StandardCharsets.UTF_8);
    }

    private static void analyze(String in, String out) throws IOException {
        TurkishMorphology m = TurkishMorphology.createWithDefaults();
        List<String> words = Files.readAllLines(Paths.get(in), StandardCharsets.UTF_8);
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(out), StandardCharsets.UTF_8)) {
            for (String word : words) {
                word = word.trim();
                if (word.isEmpty()) continue;
                WordAnalysis wa = m.analyze(word);
                Set<String> seen = new LinkedHashSet<>();
                for (SingleAnalysis s : wa) {
                    DictionaryItem d = s.getDictionaryItem();
                    seen.add(String.join("\t", word, d.lemma, d.root, d.primaryPos.toString(),
                            d.secondaryPos.toString(), d.hasAttribute(RootAttribute.Informal) ? "1" : "0"));
                }
                if (seen.isEmpty()) seen.add(word + "\t-");
                for (String row : seen) {
                    w.write(row);
                    w.write('\n');
                }
            }
        }
    }
}
