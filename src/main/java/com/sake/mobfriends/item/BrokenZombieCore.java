package com.sake.mobfriends.item;

import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag; // <-- 添加导入
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // <-- 添加导入
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class BrokenZombieCore extends AbstractCoreItem {

    public BrokenZombieCore(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, @NotNull InteractionHand pUsedHand) {
        ItemStack heldItem = pPlayer.getItemInHand(pUsedHand);
        ItemStack offhandItem = pPlayer.getItemInHand(InteractionHand.OFF_HAND);

        if (offhandItem.is(Items.TOTEM_OF_UNDYING)) {
            ItemStack activeCore = new ItemStack(ModItems.ACTIVE_ZOMBIE_CORE.get());
            UUID zombieUUID = getZombieUUID(heldItem);

            // --- 【核心修正：修复核心但不召唤】 ---

            // 1. 准备好修复后的核心，并把UUID传过去
            if (zombieUUID != null) {
                setZombieUUID(activeCore, zombieUUID);
            }

            // 2. 创建一个“空的”僵尸NBT数据，并存入修复好的核心中
            //    这会让核心立刻进入“已储存”状态，可以被右键召唤
            CompoundTag freshNbt = new CompoundTag();
            // 写入最重要的信息：实体ID，这样游戏才知道要生成什么
            freshNbt.putString("id", EntityType.getKey(com.sake.mobfriends.init.ModEntities.COMBAT_ZOMBIE.get()).toString());
            activeCore.set(ModDataComponents.STORED_ZOMBIE_NBT.get(), freshNbt);


            // 3. 在客户端播放动画
            if (pLevel.isClientSide()) {
                Minecraft.getInstance().gameRenderer.displayItemActivation(activeCore);
            }

            // 4. 在服务端消耗不死图腾
            if (!pLevel.isClientSide()) {
                if (!pPlayer.getAbilities().instabuild) {
                    offhandItem.shrink(1);
                }
                // 只播放音效，不生成实体
                pLevel.playSound(null, pPlayer.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            // 5. 将玩家手中的破损核心替换为“已修复并储存好”的激活核心
            pPlayer.setItemInHand(pUsedHand, activeCore);
            return InteractionResultHolder.sidedSuccess(activeCore, pLevel.isClientSide());
        }
        return InteractionResultHolder.fail(heldItem);
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        pTooltipComponents.add(Component.translatable("tooltip.mob_friends.broken_zombie_core").withStyle(ChatFormatting.RED));
        pTooltipComponents.add(Component.translatable("tooltip.mob_friends.broken_zombie_core_repair").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }
}