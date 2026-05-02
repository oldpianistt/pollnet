package com.pollnet.poll;

import com.pollnet.common.BaseEntity;
import com.pollnet.poll.question.Question;
import com.pollnet.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "polls")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poll extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 280)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "results_visibility", nullable = false, length = 16)
    private ResultsVisibility resultsVisibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "open_answers_visibility", nullable = false, length = 16)
    private OpenAnswersVisibility openAnswersVisibility;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    public void addQuestion(Question q) {
        q.setPoll(this);
        questions.add(q);
    }
}
