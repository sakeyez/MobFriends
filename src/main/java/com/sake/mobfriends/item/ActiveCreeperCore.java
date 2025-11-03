package com.sake.mobfriends.item;

import com.sake.mobfriends.client.gui.CombatCoreTooltip;
import com.sake.mobfriends.entity.CombatCreeper;
import com.sake.mobfriends.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ActiveCreeperCore extends Item {
    public ActiveCreeperCore(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide() || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        CompoundTag storedNBT = stack.get(ModDataComponents.STORED_CREEPER_NBT.get());
        UUID storedUUID = stack.get(ModDataComponents.CREEPER_UUID.get());

        if (storedNBT != null && storedUUID != null && level instanceof ServerLevel serverLevel) {
            Entity entity = EntityType.loadEntityRecursive(storedNBT, level, e -> {
                e.setUUID(storedUUID);
                e.setPos(Vec3.atCenterOf(context.getClickedPos().above()));
                return e;
            });
            if (entity != null) {
                serverLevel.addFreshEntity(entity);
                stack.remove(ModDataComponents.STORED_CREEPER_NBT.get());
                level.playSound(null, context.getClickedPos(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    public static void storeCreeper(ItemStack stack, CombatCreeper creeper) {
        if (creeper.isAlive()) {
            CompoundTag nbt = creeper.saveWithoutId(new CompoundTag());
            nbt.putString("id", EntityType.getKey(creeper.getType()).toString());
            stack.set(ModDataComponents.STORED_CREEPER_NBT.get(), nbt);
        }
    }

    @Override
    public Component getName(ItemStack pStack) {
        // .plainCopy() 获取 lang 文件中的原始文本
        // .withStyle(ChatFormatting.GOLD) 将其设为橙色 (GOLD 在 MC 中是橙色)
        return super.getName(pStack).plainCopy().withStyle(ChatFormatting.GOLD);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        CompoundTag nbt = stack.get(ModDataComponents.STORED_CREEPER_NBT.get());
        if (nbt != null) {
            // 【修改】读取所有需要的数据
            int level = nbt.getInt("WarriorLevel");
            int happiness = nbt.getInt("WarriorHappiness");
            int levelCap = nbt.getInt("WarriorLevelCap");
            int diningFoodsCount = nbt.contains("EatenDiningFoods", 9) ? nbt.getList("EatenDiningFoods", 8).size() : 0;
            int ritualFoodsCount = nbt.contains("EatenRitualFoods", 9) ? nbt.getList("EatenRitualFoods", 8).size() : 0;

            return Optional.of(new CombatCoreTooltip(nbt, level, happiness, levelCap, diningFoodsCount, ritualFoodsCount));
        }
        return Optional.empty();
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return pStack.has(ModDataComponents.STORED_CREEPER_NBT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(ModDataComponents.STORED_CREEPER_NBT.get())) {
            tooltip.add(Component.translatable("tooltip.mob_friends.active_zombie_core.stored").withStyle(ChatFormatting.BLUE));
        } else {
            tooltip.add(Component.translatable("tooltip.mob_friends.active_zombie_core.linked").withStyle(ChatFormatting.GREEN));
        }
    }
}