package com.memospark.core.service;

import com.memospark.core.domain.*;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import com.memospark.core.repository.DeckRepository;
import com.memospark.core.repository.JobJdRepository;
import com.memospark.core.repository.TargetSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TargetSkillServiceTest {

    @Mock private AiService aiService;
    @Mock private JobJdRepository jobJdRepository;
    @Mock private TargetSkillRepository targetSkillRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CardProgressRepository cardProgressRepository;
    @Mock private SpacedRepetitionService spacedRepetitionService;
    @Mock private DeckService deckService;

    @InjectMocks
    private TargetSkillService service;

    private User user;
    private Target target;

    @BeforeEach
    void setUp() {
        user = new User("u", "p", UserRole.USER);
        user.setId(1L);
        target = new Target(user, "SRE", "ACME");
        target.setId(10L);
    }

    @Test
    void analyzeAndPersist_highConfidenceDeckMatch_reusesExistingDeck() {
        Deck existing = new Deck("Kubernetes 面试核心", "Pod、调度、网络、存储与控制器排障", DeckType.CUSTOM, user);
        existing.setId(20L);
        Card card = new Card(existing, "Pod 调度失败怎么排查？", "看 scheduler 事件、资源和亲和性。", "kubernetes,pod");

        when(jobJdRepository.findByTargetIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(new JobJd(target, "jd", "K8s 集群运维，Pod、网络、存储排障", "boss")));
        when(aiService.analyzeJds(anyList(), eq("zh"), eq(1L))).thenReturn(analysis("Kubernetes 深度运维"));
        when(deckRepository.findByUserId(1L)).thenReturn(List.of(existing));
        when(cardRepository.findDistinctTagsByDeckId(20L)).thenReturn(List.of("kubernetes,k8s,pod"));
        when(cardRepository.findByDeckId(20L)).thenReturn(List.of(card));
        when(targetSkillRepository.save(any(TargetSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetSkill saved = service.analyzeAndPersist(target, "zh", false).get(0);

        assertSame(existing, saved.getDeck());
        assertEquals(DeckLinkSource.MATCHED_EXISTING, saved.getDeckLinkSource());
        assertNotNull(saved.getDeckMatchScore());
        assertTrue(saved.getDeckMatchScore() >= 0.72);
        verify(deckRepository, never()).save(any(Deck.class));
    }

    @Test
    void analyzeAndPersist_lowConfidenceMatch_createsNewDeck() {
        Deck existing = new Deck("计算机网络与网关", "TCP/IP、HTTP、TLS、DNS", DeckType.CUSTOM, user);
        existing.setId(21L);
        when(jobJdRepository.findByTargetIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(new JobJd(target, "jd", "K8s 集群运维", "boss")));
        when(aiService.analyzeJds(anyList(), eq("zh"), eq(1L))).thenReturn(analysis("Kubernetes 深度运维"));
        when(deckRepository.findByUserId(1L)).thenReturn(List.of(existing));
        when(cardRepository.findDistinctTagsByDeckId(21L)).thenReturn(List.of("tcp,http"));
        when(cardRepository.findByDeckId(21L)).thenReturn(List.of());
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> {
            Deck deck = inv.getArgument(0);
            deck.setId(30L);
            return deck;
        });
        when(targetSkillRepository.save(any(TargetSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetSkill saved = service.analyzeAndPersist(target, "zh", false).get(0);

        assertEquals("Kubernetes 深度运维", saved.getDeck().getName());
        assertEquals(DeckLinkSource.AI_CREATED, saved.getDeckLinkSource());
        assertNull(saved.getDeckMatchScore());
        verify(deckRepository).save(any(Deck.class));
    }

    @Test
    void deleteSkillWithDeck_matchedExistingDeck_doesNotDeleteDeck() {
        Deck existing = new Deck("Kubernetes 面试核心", "", DeckType.CUSTOM, user);
        existing.setId(20L);
        TargetSkill skill = new TargetSkill(target, user, "Kubernetes 深度运维", null, null, 4);
        skill.setDeck(existing);
        skill.setDeckLinkSource(DeckLinkSource.MATCHED_EXISTING);

        service.deleteSkillWithDeck(skill);

        verify(targetSkillRepository).delete(skill);
        verify(deckService, never()).deleteDeck(anyLong());
    }

    @Test
    void deleteSkillWithDeck_aiCreatedDeck_deletesDeck() {
        Deck generated = new Deck("Kubernetes 深度运维", "", DeckType.CUSTOM, user);
        generated.setId(22L);
        TargetSkill skill = new TargetSkill(target, user, "Kubernetes 深度运维", null, null, 4);
        skill.setDeck(generated);
        skill.setDeckLinkSource(DeckLinkSource.AI_CREATED);

        service.deleteSkillWithDeck(skill);

        verify(targetSkillRepository).delete(skill);
        verify(deckService).deleteDeck(22L);
    }

    private Map<String, Object> analysis(String name) {
        return Map.of("decks", List.of(Map.of(
                "name", name,
                "description", "K8s 集群规划、调度、网络、存储和排障",
                "topics", List.of("Pod 调度", "CNI 网络", "CSI 存储"),
                "suggestedCardCount", 12
        )));
    }
}
