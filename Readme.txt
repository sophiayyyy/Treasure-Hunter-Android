Known flaws:
1. Flash has a compatibility problem. It only perfectly works on a lower SDK version like sdk18. For a higher SDK version, it may turn on occasionally.
2. When deleting a post, the post will not be removed from the favorite folder if a user liked the post before. We have implemented this part of the code, but we commented out because the performance of our app will drop significantly when it tries to search the post in every user’s favorite list. 


Bonus:
• Use of RecyclerView and CardView. 
• Good use of Menus.
• Using Gestures (Swipe left to delete post).
• Targeting multiple locales(English and Chinese).