package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.config.Settings.ModifierMode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierVoteTest {
	private static final List<String> A = List.of("uhc");
	private static final List<String> B = List.of("horde");

	@Test
	void majorityWins() {
		ModifierVote vote = new ModifierVote(List.of(A, B));
		vote.cast(UUID.randomUUID(), 2);
		vote.cast(UUID.randomUUID(), 2);
		vote.cast(UUID.randomUUID(), 1);
		assertEquals(B, vote.winner(new Random()));
	}

	@Test
	void changingVoteReplacesTheOldOne() {
		ModifierVote vote = new ModifierVote(List.of(A, B));
		UUID player = UUID.randomUUID();
		vote.cast(player, 1);
		vote.cast(player, 2);
		assertArrayEquals(new int[] {0, 1}, vote.counts());
	}

	@Test
	void outOfRangeVoteIsRejected() {
		ModifierVote vote = new ModifierVote(List.of(A, B));
		assertFalse(vote.cast(UUID.randomUUID(), 0));
		assertFalse(vote.cast(UUID.randomUUID(), 3));
		assertTrue(vote.cast(UUID.randomUUID(), 2));
	}

	@Test
	void tiesAreBrokenBothWays() {
		ModifierVote vote = new ModifierVote(List.of(A, B));
		vote.cast(UUID.randomUUID(), 1);
		vote.cast(UUID.randomUUID(), 2);
		Random random = new Random(5);
		Set<List<String>> winners = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			winners.add(vote.winner(random));
		}
		assertEquals(Set.of(A, B), winners);
	}

	@Test
	void selectorOffModeAndEmptyPoolGiveNothing() {
		ModifierSelector selector = new ModifierSelector(new Random(6));
		assertEquals(List.of(), selector.pickForNextRun(ModifierMode.OFF, Set.of("uhc"), 1));
		assertEquals(List.of(), selector.pickForNextRun(ModifierMode.RANDOM, Set.of(), 1));
		assertNull(selector.openVote(ModifierMode.VOTE, Set.of(), 1));
	}

	@Test
	void forceAppliesToOneRunOnly() {
		ModifierSelector selector = new ModifierSelector(new Random(7));
		selector.force(List.of("swap"));
		assertEquals(List.of("swap"), selector.pickForNextRun(ModifierMode.OFF, Set.of(), 1));
		assertEquals(List.of(), selector.pickForNextRun(ModifierMode.OFF, Set.of(), 1));
	}

	@Test
	void selectorUsesVoteResultThenClearsIt() {
		ModifierSelector selector = new ModifierSelector(new Random(8));
		ModifierVote vote = selector.openVote(ModifierMode.VOTE, Set.of("uhc", "horde"), 1);
		assertNotNull(vote);
		vote.cast(UUID.randomUUID(), 2);
		List<String> expected = vote.options().get(1);
		assertEquals(expected, selector.pickForNextRun(ModifierMode.VOTE, Set.of("uhc", "horde"), 1));
		assertNull(selector.vote());
	}
}
